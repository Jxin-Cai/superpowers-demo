# 迭代2设计文档：用户权限与内容搜索

## 1. 认证与权限架构

采用 **Spring Security + HTTP Basic Auth + 登录页面**：

### 角色定义

| 角色 | 描述 | 权限 |
|------|------|------|
| `ROLE_ADMIN` | 超级管理员 | 后台全部 CRUD，前台全部 |
| `ROLE_USER` | 普通注册用户 | 前台全部，个人资料修改 |
| 游客 | 未登录 | 前台只读 |

### 安全配置规则

- `/login`, `/register`, `/api/auth/**` → 所有用户可访问
- `/admin/**`, `/api/admin/**` → 仅 `ROLE_ADMIN` 可访问
- `/api/public/**` → 所有用户可访问
- 其他请求 → 需要登录

### 前后端交互流程

1. 用户访问需登录的页面
2. 后端检测未认证，重定向到登录页（`/login`）
3. 用户登录成功后，跳转到前台首页（`/`）
4. 前端存储 username + password 到 localStorage
5. 后续请求通过 Axios 拦截器添加 `Authorization: Basic xxx` 头

## 2. 数据库设计

### 新增表：users

```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,  -- BCrypt 加密
    email VARCHAR(100),
    role ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    status ENUM('ACTIVE', 'DISABLED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
```

### 修改表：articles

```sql
ALTER TABLE articles ADD COLUMN keywords VARCHAR(500) COMMENT '逗号分隔的关键词';
ALTER TABLE articles ADD COLUMN author_id BIGINT COMMENT '作者ID';
ALTER TABLE articles ADD CONSTRAINT fk_articles_author FOREIGN KEY (author_id) REFERENCES users(id);
```

### DDD 实体结构

```
domain/model/user/
├── User.java              # 用户实体（聚合根）
├── Role.java              # 角色枚举
├── Status.java            # 状态枚举
└── Password.java          # 密码值对象（BCrypt）
```

## 3. 后端 API 设计

### 认证相关 API (`presentation/controller/auth/`)

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/api/auth/login` | 验证用户名密码 | 游客 |
| POST | `/api/auth/register` | 普通用户注册 | 游客 |
| GET | `/api/auth/current` | 获取当前登录用户 | 已登录 |

### 用户管理 API (`presentation/controller/admin/`)

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/admin/users` | 用户列表（分页） | ADMIN |
| GET | `/api/admin/users/{id}` | 用户详情 | ADMIN |
| POST | `/api/admin/users` | 创建用户 | ADMIN |
| PUT | `/api/admin/users/{id}` | 更新用户 | ADMIN |
| DELETE | `/api/admin/users/{id}` | 删除用户 | ADMIN |

### 前台搜索 API (`presentation/controller/publicapi/`)

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/api/public/articles/search` | 文章搜索 | 所有 |

**搜索参数：**
- `keyword`: 搜索关键词（标题或关键词字段匹配）
- `categoryId`: 可选，分类筛选
- `page`, `size`: 分页参数

## 4. 前端组件设计

### 新增组件

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
    └── UserMenu.vue           # 用户菜单（显示用户名/退出）
```

### useAuth Composable

```javascript
const { isLoggedIn, username, login, logout, register } = useAuth()

// login(username, password): 存储到 localStorage
// logout(): 清除 localStorage
// register(username, password, email): 注册
```

## 5. AOP 用户上下文

### UserContext（ThreadLocal 容器）

```java
public class UserContext {
    private static final ThreadLocal<UserId> CURRENT = new ThreadLocal<>();

    public static void setCurrent(UserId userId) {
        CURRENT.set(userId);
    }

    public static UserId getCurrentUser() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
```

### @CurrentUser 注解

```java
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {}
```

### AOP 切面

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

### 使用示例

```java
@PostMapping("/articles")
public Article create(@RequestBody ArticleDto dto, @CurrentUser UserId userId) {
    return articleService.create(dto, userId);
}
```

## 6. Spring Security 配置

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
                .defaultSuccessUrl("/", true)  // 登录成功跳转到前台首页
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

## 7. 搜索功能实现

### ArticleRepository 搜索方法

```java
@Query("SELECT a FROM Article a WHERE " +
       "(:keyword IS NULL OR a.title LIKE %:keyword% OR a.keywords LIKE %:keyword%) " +
       "AND (:categoryId IS NULL OR a.category.id = :categoryId)")
Page<Article> search(@Param("keyword") String keyword,
                     @Param("categoryId") Long categoryId,
                     Pageable pageable);
```

## 8. 文件结构总结

### 后端新增文件

```
src/main/java/com/example/cms/
├── domain/
│   └── model/user/
│       ├── User.java
│       ├── Role.java
│       ├── Status.java
│       └── Password.java
├── application/
│   └── service/
│       ├── AuthService.java
│       └── UserApplicationService.java
├── infrastructure/
│   └── persistence/
│       ├── entity/UserJpaEntity.java
│       └── repository/UserRepositoryImpl.java
├── presentation/
│   ├── controller/auth/AuthController.java
│   ├── controller/admin/UserController.java
│   └── dto/
└── config/
    ├── SecurityConfig.java
    └── UserContextAspect.java
```

### 前端新增文件

```
frontend/src/
├── auth/
│   ├── Login.vue
│   └── Register.vue
├── admin/views/
│   ├── UserList.vue
│   └── UserForm.vue
├── public/views/
│   └── SearchArticle.vue
├── composables/
│   └── useAuth.js
└── components/
    └── UserMenu.vue
```
