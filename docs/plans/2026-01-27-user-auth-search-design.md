# 迭代2：用户权限与内容搜索 - 设计文档

## 1. 概述

本设计实现用户认证授权系统和前台搜索功能，采用 HTTP Basic 认证 + Spring Security。

## 2. 认证与权限架构

### 2.1 角色定义

| 角色 | 描述 | 权限 |
|------|------|------|
| `ROLE_ADMIN` | 超级管理员 | 后台全部 CRUD，前台全部 |
| `ROLE_USER` | 普通注册用户 | 前台全部，个人资料修改 |
| 游客 | 未登录 | 前台只读 |

### 2.2 安全配置

- `/admin/**` → 仅 `ROLE_ADMIN` 可访问
- `/api/admin/**` → 仅 `ROLE_ADMIN` 可访问
- `/api/user/**`（个人资料）→ `ROLE_USER` 或 `ROLE_ADMIN`
- `/api/public/**` → 所有用户（含游客）
- `/api/auth/**` → 登录/注册接口

### 2.3 认证流程

1. **后端控制**：Spring Security 拦截未认证请求，重定向到登录页
2. **前端存储**：登录成功后存储 username + password（localStorage）
3. **请求携带**：每次请求通过 Axios 拦截器添加 Basic Auth 头

## 3. 数据模型设计

### 3.1 新增 users 表

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
```

### 3.2 修改 articles 表

```sql
ALTER TABLE articles ADD COLUMN keywords VARCHAR(500) COMMENT '逗号分隔的关键词';
ALTER TABLE articles ADD COLUMN author_id BIGINT COMMENT '作者ID';
ALTER TABLE articles ADD CONSTRAINT fk_articles_author FOREIGN KEY (author_id) REFERENCES users(id);
```

### 3.3 DDD 实体结构

```
domain/model/user/
├── User.java              # 用户聚合根
├── Role.java              # 角色枚举
├── Status.java            # 状态枚举
└── Password.java          # 密码值对象（BCrypt）
```

## 4. API 设计

### 4.1 认证 API

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 验证用户名密码 | 游客 |
| POST | `/api/auth/register` | 普通用户注册 | 游客 |
| GET | `/api/auth/current` | 获取当前登录用户 | 已登录 |

### 4.2 用户管理 API（Admin）

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/admin/users` | 用户列表（分页） | ADMIN |
| GET | `/api/admin/users/{id}` | 用户详情 | ADMIN |
| POST | `/api/admin/users` | 创建用户 | ADMIN |
| PUT | `/api/admin/users/{id}` | 更新用户 | ADMIN |
| DELETE | `/api/admin/users/{id}` | 删除用户 | ADMIN |

### 4.3 搜索 API

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/public/articles/search` | 文章搜索 | 所有 |

**搜索参数：**
- `keyword`: 搜索关键词（标题或关键词字段匹配）
- `categoryId`: 可选，分类筛选
- `page`, `size`: 分页参数

## 5. 前端组件设计

### 5.1 新增组件

```
frontend/src/
├── auth/
│   ├── Login.vue              # 登录页面
│   └── Register.vue           # 注册页面
├── admin/
│   └── views/
│       ├── UserList.vue       # 用户管理页面
│       └── UserForm.vue       # 用户表单
├── public/
│   └── views/
│       └── SearchArticle.vue  # 文章搜索页面
├── composables/
│   └── useAuth.js             # 认证状态管理
└── components/
    └── UserMenu.vue           # 用户菜单
```

### 5.2 useAuth Composable

```javascript
const { isLoggedIn, username, login, logout, register } = useAuth()

// login(): 存储 username + password 到 localStorage
// logout(): 清除 localStorage
// isLoggedIn(): 检查 localStorage 是否有凭据
```

### 5.3 路由配置

| 路径 | 组件 | 描述 |
|------|------|------|
| `/login` | Login.vue | 登录页 |
| `/register` | Register.vue | 注册页 |
| `/admin/*` | Admin组件 | 后台（后端守卫） |
| `/search` | SearchArticle.vue | 搜索页 |

## 6. 后端实现细节

### 6.1 UserContext（ThreadLocal）

```java
public class UserContext {
    private static final ThreadLocal<UserId> CURRENT = new ThreadLocal<>();

    public static void setCurrent(UserId userId) { CURRENT.set(userId); }
    public static UserId getCurrentUser() { return CURRENT.get(); }
    public static void clear() { CURRENT.remove(); }
}
```

### 6.2 AOP 切面（确保异常时清除）

```java
@Aspect
@Component
public class UserContextAspect {
    @Around("@annotation(org.springframework.web.bind.annotation.*Mapping)")
    public Object setUserContext(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                UserId userId = extractUserId(auth);
                UserContext.setCurrent(userId);
            }
            return pjp.proceed();
        } finally {
            UserContext.clear();  // finally 确保异常时也能清除
        }
    }
}
```

### 6.3 Spring Security 配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/register", "/api/auth/**").permitAll()
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(withDefaults())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 6.4 搜索实现

```java
@Query("SELECT a FROM Article a WHERE " +
       "(:keyword IS NULL OR a.title LIKE %:keyword% OR a.keywords LIKE %:keyword%) " +
       "AND (:categoryId IS NULL OR a.category.id = :categoryId)")
Page<Article> search(@Param("keyword") String keyword,
                     @Param("categoryId") Long categoryId,
                     Pageable pageable);
```

## 7. 技术选型

| 需求 | 技术方案 |
|------|----------|
| 认证方式 | HTTP Basic Auth |
| 框架 | Spring Security |
| 密码加密 | BCrypt |
| 前端状态 | localStorage |
| 搜索方式 | JPA LIKE 查询 |
| 关键词输入 | 逗号分隔文本框 |
