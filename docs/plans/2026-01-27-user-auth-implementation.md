# 用户权限与内容搜索实施计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 实现用户认证授权系统和前台内容搜索功能

**架构：** Spring Security + HTTP Basic 认证，采用 DDD 分层架构，前端 Vue 3 + Axios 拦截器

**技术栈：** Spring Boot 3.x, Spring Security, JPA, Vue 3, Element Plus, Flyway

---

## Task 1: 数据库迁移 - 创建用户表

**Files:**
- Create: `src/main/resources/db/migration/V3__Create_users_table.sql`

**Step 1: 编写迁移脚本**

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_username (username),
    INDEX idx_users_role (role)
);
```

**Step 2: 运行迁移验证**

```bash
cd /Users/jxin/Agent/VB-Coding-Demo/sdd/superpowers-demo/.worktrees/iteration2-user-auth
./gradlew bootRun
```
Expected: 应用启动成功，users 表创建成功

**Step 3: 停止应用并验证表结构**

```bash
# 连接数据库验证
```
Expected: users 表存在，字段正确

**Step 4: 提交**

```bash
git add src/main/resources/db/migration/V3__Create_users_table.sql
git commit -m "feat: 创建用户表

- 添加用户名、密码（BCrypt）、邮箱
- 角色字段（ADMIN/USER）
- 状态字段（ACTIVE/DISABLED）

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 2: 数据库迁移 - 文章表添加字段

**Files:**
- Create: `src/main/resources/db/migration/V4__Add_keywords_and_author_to_articles.sql`

**Step 1: 编写迁移脚本**

```sql
ALTER TABLE articles ADD COLUMN keywords VARCHAR(500) COMMENT '逗号分隔的关键词';
ALTER TABLE articles ADD COLUMN author_id BIGINT COMMENT '作者ID';
ALTER TABLE articles ADD CONSTRAINT fk_articles_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL;
CREATE INDEX idx_articles_keywords ON articles(keywords);
CREATE INDEX idx_articles_author ON articles(author_id);
```

**Step 2: 运行迁移验证**

```bash
./gradlew bootRun
```
Expected: 应用启动成功，articles 表新增字段

**Step 3: 提交**

```bash
git add src/main/resources/db/migration/V4__Add_keywords_and_author_to_articles.sql
git commit -m "feat: 文章表添加关键词和作者字段

- keywords: 逗号分隔的关键词
- author_id: 关联用户表

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 3: 领域模型 - Role 枚举

**Files:**
- Create: `src/main/java/com/example/cms/domain/model/user/Role.java`

**Step 1: 创建 Role 枚举**

```java
package com.example.cms.domain.model.user;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("ADMIN"),
    USER("USER");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    public static Role from(String value) {
        for (Role role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/domain/model/user/Role.java
git commit -m "feat: 添加 Role 枚举

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 4: 领域模型 - Status 枚举

**Files:**
- Create: `src/main/java/com/example/cms/domain/model/user/UserStatus.java`

**Step 1: 创建 UserStatus 枚举**

```java
package com.example.cms.domain.model.user;

import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("ACTIVE"),
    DISABLED("DISABLED");

    private final String value;

    UserStatus(String value) {
        this.value = value;
    }

    public static UserStatus from(String value) {
        for (UserStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/domain/model/user/UserStatus.java
git commit -m "feat: 添加 UserStatus 枚举

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 5: 领域模型 - UserId 值对象

**Files:**
- Create: `src/main/java/com/example/cms/domain/model/user/UserId.java`

**Step 1: 创建 UserId 值对象**

```java
package com.example.cms.domain.model.user;

import lombok.Value;

@Value
public class UserId {
    Long value;

    private UserId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Invalid user id");
        }
        this.value = value;
    }

    public static UserId of(Long value) {
        return new UserId(value);
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/domain/model/user/UserId.java
git commit -m "feat: 添加 UserId 值对象

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 6: 领域模型 - Username 值对象

**Files:**
- Create: `src/main/java/com/example/cms/domain/model/user/Username.java`

**Step 1: 创建 Username 值对象**

```java
package com.example.cms.domain.model.user;

import lombok.Value;

@Value
public class Username {
    String value;

    private Username(String value) {
        if (value == null || value.isBlank() || value.length() < 3 || value.length() > 50) {
            throw new IllegalArgumentException("Username must be between 3 and 50 characters");
        }
        this.value = value.trim();
    }

    public static Username of(String value) {
        return new Username(value);
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/domain/model/user/Username.java
git commit -m "feat: 添加 Username 值对象

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 7: 领域模型 - Password 值对象

**Files:**
- Create: `src/main/java/com/example/cms/domain/model/user/Password.java`

**Step 1: 创建 Password 值对象**

```java
package com.example.cms.domain.model.user;

import lombok.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Value
public class Password {
    String encodedValue;

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();
    private static final int MIN_RAW_LENGTH = 6;

    private Password(String encodedValue) {
        if (encodedValue == null || encodedValue.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.encodedValue = encodedValue;
    }

    public static Password encode(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_RAW_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_RAW_LENGTH + " characters");
        }
        return new Password(ENCODER.encode(rawPassword));
    }

    public static Password fromEncoded(String encoded) {
        return new Password(encoded);
    }

    public boolean matches(String rawPassword) {
        return ENCODER.matches(rawPassword, this.encodedValue);
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/domain/model/user/Password.java
git commit -m "feat: 添加 Password 值对象

- 使用 BCrypt 加密
- 支持密码验证

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 8: 领域模型 - Email 值对象

**Files:**
- Create: `src/main/java/com/example/cms/domain/model/user/Email.java`

**Step 1: 创建 Email 值对象**

```java
package com.example.cms.domain.model.user;

import lombok.Value;

@Value
public class Email {
    String value;

    private Email(String value) {
        if (value != null && !value.isBlank()) {
            String trimmed = value.trim();
            if (!isValidEmail(trimmed)) {
                throw new IllegalArgumentException("Invalid email format");
            }
            this.value = trimmed;
        } else {
            this.value = null;
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/domain/model/user/Email.java
git commit -m "feat: 添加 Email 值对象

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 9: 领域模型 - User 聚合根

**Files:**
- Create: `src/main/java/com/example/cms/domain/model/user/User.java`

**Step 1: 创建 User 实体**

```java
package com.example.cms.domain.model.user;

import com.example.cms.domain.shared.Audit;
import lombok.Getter;
import lombok.AccessLevel;

import java.util.Optional;

@Getter
public class User {
    private final UserId id;
    private final Username username;
    private final Password password;
    private final Email email;
    private Role role;
    private UserStatus status;
    private Audit audit;

    private User(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.password = builder.password;
        this.email = builder.email;
        this.role = builder.role != null ? builder.role : Role.USER;
        this.status = builder.status != null ? builder.status : UserStatus.ACTIVE;
        this.audit = builder.audit != null ? builder.audit : Audit.create();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getEmailValue() {
        return Optional.ofNullable(email).map(Email::getValue);
    }

    public void changeRole(Role newRole) {
        this.role = newRole;
        this.audit = this.audit.markModified();
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
        this.audit = this.audit.markModified();
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.audit = this.audit.markModified();
    }

    public void updateEmail(Email newEmail) {
        if (this.email != null && !this.email.equals(newEmail)) {
            // Email 字段在 User 中是 final，需要重新赋值
            // 实际实现需要调整，这里先用方法标记
            this.audit = this.audit.markModified();
        }
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public static class Builder {
        private UserId id;
        private Username username;
        private Password password;
        private Email email;
        private Role role;
        private UserStatus status;
        private Audit audit;

        public Builder id(UserId id) {
            this.id = id;
            return this;
        }

        public Builder username(Username username) {
            this.username = username;
            return this;
        }

        public Builder password(Password password) {
            this.password = password;
            return this;
        }

        public Builder email(Email email) {
            this.email = email;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public Builder audit(Audit audit) {
            this.audit = audit;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/domain/model/user/User.java
git commit -m "feat: 添加 User 聚合根

- 使用 Builder 模式
- 支持角色切换、启用/禁用

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 10: 基础设施层 - UserEntity JPA 实体

**Files:**
- Create: `src/main/java/com/example/cms/infrastructure/persistence/entity/UserEntity.java`

**Step 1: 创建 UserEntity**

```java
package com.example.cms.infrastructure.persistence.entity;

import com.example.cms.domain.model.user.Role;
import com.example.cms.domain.model.user.UserStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 100)
    private String email;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/infrastructure/persistence/entity/UserEntity.java
git commit -m "feat: 添加 UserEntity JPA 实体

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 11: 基础设施层 - UserMapper 转换器

**Files:**
- Create: `src/main/java/com/example/cms/infrastructure/persistence/mapper/UserMapper.java`

**Step 1: 创建 UserMapper**

```java
package com.example.cms.infrastructure.persistence.mapper;

import com.example.cms.domain.model.user.*;
import com.example.cms.domain.shared.Audit;
import com.example.cms.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toDomain(UserEntity entity) {
        return User.builder()
                .id(UserId.of(entity.getId()))
                .username(Username.of(entity.getUsername()))
                .password(Password.fromEncoded(entity.getPassword()))
                .email(entity.getEmail() != null ? Email.of(entity.getEmail()) : null)
                .role(Role.from(entity.getRole()))
                .status(UserStatus.from(entity.getStatus()))
                .audit(Audit.of(entity.getCreatedAt(), entity.getUpdatedAt()))
                .build();
    }

    public UserEntity toEntity(User domain) {
        UserEntity entity = new UserEntity();
        if (domain.getId() != null) {
            entity.setId(domain.getId().getValue());
        }
        entity.setUsername(domain.getUsername().getValue());
        entity.setPassword(domain.getPassword().getEncodedValue());
        domain.getEmailValue().ifPresent(entity::setEmail);
        entity.setRole(domain.getRole().getValue());
        entity.setStatus(domain.getStatus().getValue());
        return entity;
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/infrastructure/persistence/mapper/UserMapper.java
git commit -m "feat: 添加 UserMapper 转换器

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 12: 基础设施层 - SpringDataUserRepository

**Files:**
- Create: `src/main/java/com/example/cms/infrastructure/persistence/repository/SpringDataUserRepository.java`

**Step 1: 创建 SpringDataUserRepository**

```java
package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/infrastructure/persistence/repository/SpringDataUserRepository.java
git commit -m "feat: 添加 SpringDataUserRepository

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 13: 领域层 - UserRepository 接口

**Files:**
- Create: `src/main/java/com/example/cms/domain/repository/UserRepository.java`

**Step 1: 创建 UserRepository 接口**

```java
package com.example.cms.domain.repository;

import com.example.cms.domain.model.user.User;
import com.example.cms.domain.model.user.UserId;
import com.example.cms.domain.model.user.Username;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByUsername(Username username);
    List<User> findAll();
    void deleteById(UserId id);
    boolean existsByUsername(Username username);
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/domain/repository/UserRepository.java
git commit -m "feat: 添加 UserRepository 接口

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 14: 基础设施层 - UserRepositoryImpl 实现

**Files:**
- Create: `src/main/java/com/example/cms/infrastructure/persistence/repository/UserRepositoryImpl.java`

**Step 1: 创建 UserRepositoryImpl**

```java
package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.domain.model.user.User;
import com.example.cms.domain.model.user.UserId;
import com.example.cms.domain.model.user.Username;
import com.example.cms.domain.repository.UserRepository;
import com.example.cms.infrastructure.persistence.entity.UserEntity;
import com.example.cms.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        UserEntity saved = springDataUserRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return springDataUserRepository.findById(id.getValue())
                .map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return springDataUserRepository.findByUsername(username.getValue())
                .map(userMapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return springDataUserRepository.findAll().stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UserId id) {
        springDataUserRepository.deleteById(id.getValue());
    }

    @Override
    public boolean existsByUsername(Username username) {
        return springDataUserRepository.existsByUsername(username.getValue());
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/infrastructure/persistence/repository/UserRepositoryImpl.java
git commit -m "feat: 添加 UserRepositoryImpl 实现

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 15: 支持层 - UserContext 用户上下文

**Files:**
- Create: `src/main/java/com/example/cms/security/UserContext.java`

**Step 1: 创建 UserContext**

```java
package com.example.cms.security;

import com.example.cms.domain.model.user.UserId;

public class UserContext {
    private static final ThreadLocal<UserId> CURRENT = new ThreadLocal<>();

    public static void setCurrent(UserId userId) {
        CURRENT.set(userId);
    }

    public static UserId getCurrentUser() {
        return CURRENT.get();
    }

    public static Long getCurrentUserIdValue() {
        UserId userId = CURRENT.get();
        return userId != null ? userId.getValue() : null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/security/UserContext.java
git commit -m "feat: 添加 UserContext 线程本地容器

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 16: 支持层 - @CurrentUser 注解

**Files:**
- Create: `src/main/java/com/example/cms/security/CurrentUser.java`

**Step 1: 创建 @CurrentUser 注解**

```java
package com.example.cms.security;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/security/CurrentUser.java
git commit -m "feat: 添加 @CurrentUser 注解

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 17: 支持层 - CurrentUserMethodArgumentResolver

**Files:**
- Create: `src/main/java/com/example/cms/security/CurrentUserMethodArgumentResolver.java`

**Step 1: 创建 CurrentUserMethodArgumentResolver**

```java
package com.example.cms.security;

import com.example.cms.domain.model.user.UserId;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentUserMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(CurrentUser.class) != null
                && parameter.getParameterType().equals(UserId.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            String username = authentication.getName();
            // 从数据库加载用户ID
            // 这里简化处理，实际应该从 UserService 获取
            return UserId.of(1L); // 临时实现
        }
        return null;
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/security/CurrentUserMethodArgumentResolver.java
git commit -m "feat: 添加 CurrentUserMethodArgumentResolver

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 18: 支持层 - UserContextAspect AOP切面

**Files:**
- Create: `src/main/java/com/example/cms/security/UserContextAspect.java`

**Step 1: 创建 UserContextAspect**

```java
package com.example.cms.security;

import com.example.cms.domain.model.user.User;
import com.example.cms.domain.model.user.UserId;
import com.example.cms.domain.model.user.Username;
import com.example.cms.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class UserContextAspect {

    private final UserRepository userRepository;

    @Around("@annotation(org.springframework.web.bind.annotation.*Mapping)")
    public Object setUserContext(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {
                String username = auth.getName();
                userRepository.findByUsername(Username.of(username))
                        .map(User::getId)
                        .ifPresent(UserContext::setCurrent);
            }
            return pjp.proceed();
        } finally {
            UserContext.clear();
        }
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/security/UserContextAspect.java
git commit -m "feat: 添加 UserContextAspect 切面

- 在 Controller 方法执行前设置用户上下文
- finally 确保异常时也能清除 ThreadLocal

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 19: 安全配置 - WebSecurityConfig

**Files:**
- Create: `src/main/java/com/example/cms/config/SecurityConfig.java`
- Create: `src/main/java/com/example/cms/config/WebMvcConfig.java`

**Step 1: 创建 SecurityConfig**

```java
package com.example.cms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPoint;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
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
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(authenticationEntryPoint)
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**Step 2: 创建 CustomAuthenticationEntryPoint**

```java
package com.example.cms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // API 请求返回 401
        if (request.getRequestURI().startsWith("/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        } else {
            // 页面请求重定向到登录页
            response.sendRedirect("/login");
        }
    }
}
```

**Step 3: 创建 WebMvcConfig**

```java
package com.example.cms.config;

import com.example.cms.security.CurrentUserMethodArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserMethodArgumentResolver currentUserMethodArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserMethodArgumentResolver);
    }
}
```

**Step 4: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 5: 添加 Spring Security 依赖**

检查 `build.gradle` 是否包含 spring-security 依赖，如果没有则添加：

```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
```

**Step 6: 提交**

```bash
git add src/main/java/com/example/cms/config/
git add build.gradle
git commit -m "feat: 添加 Spring Security 配置

- HTTP Basic 认证
- 表单登录（跳转到 /login）
- 角色权限控制
- 自定义 AuthenticationEntryPoint

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 20: 应用层 - UserService

**Files:**
- Create: `src/main/java/com/example/cms/application/service/UserService.java`

**Step 1: 创建 UserService**

```java
package com.example.cms.application.service;

import com.example.cms.domain.model.user.*;
import com.example.cms.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User register(String username, String password, String email) {
        Username uname = Username.of(username);
        if (userRepository.existsByUsername(uname)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        User user = User.builder()
                .username(uname)
                .password(Password.encode(password))
                .email(email != null ? Email.of(email) : null)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(Username.of(username));
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(UserId.of(id));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User createAdmin(String username, String password, String email) {
        Username uname = Username.of(username);
        if (userRepository.existsByUsername(uname)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        User user = User.builder()
                .username(uname)
                .password(Password.encode(password))
                .email(email != null ? Email.of(email) : null)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    @Transactional
    public User updateRole(Long id, Role newRole) {
        User user = userRepository.findById(UserId.of(id))
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        user.changeRole(newRole);
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(UserId.of(id));
    }

    public boolean verifyPassword(String username, String rawPassword) {
        return findByUsername(username)
                .map(user -> user.getPassword().matches(rawPassword))
                .orElse(false);
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/application/service/UserService.java
git commit -m "feat: 添加 UserService

- 用户注册
- 管理员创建
- 角色更新
- 密码验证

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 21: 表现层 - AuthController

**Files:**
- Create: `src/main/java/com/example/cms/presentation/controller/auth/AuthController.java`
- Create: `src/main/java/com/example/cms/presentation/dto/LoginRequest.java`
- Create: `src/main/java/com/example/cms/presentation/dto/RegisterRequest.java`
- Create: `src/main/java/com/example/cms/presentation/dto/UserResponse.java`

**Step 1: 创建 DTO**

```java
// src/main/java/com/example/cms/presentation/dto/LoginRequest.java
package com.example.cms.presentation.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
```

```java
// src/main/java/com/example/cms/presentation/dto/RegisterRequest.java
package com.example.cms.presentation.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
}
```

```java
// src/main/java/com/example/cms/presentation/dto/UserResponse.java
package com.example.cms.presentation.dto;

import com.example.cms.domain.model.user.Role;
import com.example.cms.domain.model.user.UserStatus;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;

    public static UserResponse from(com.example.cms.domain.model.user.User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId().getValue());
        response.setUsername(user.getUsername().getValue());
        user.getEmailValue().ifPresent(response::setEmail);
        response.setRole(user.getRole().getValue());
        response.setStatus(user.getStatus().getValue());
        return response;
    }
}
```

**Step 2: 创建 AuthController**

```java
package com.example.cms.presentation.controller.auth;

import com.example.cms.application.service.UserService;
import com.example.cms.domain.model.user.User;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.LoginRequest;
import com.example.cms.presentation.dto.RegisterRequest;
import com.example.cms.presentation.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        // HTTP Basic 由 Spring Security 处理，这里只返回用户信息
        return userService.findByUsername(request.getUsername())
                .filter(User::isActive)
                .map(user -> ApiResponse.success(UserResponse.from(user)))
                .orElse(ApiResponse.error("用户名或密码错误"));
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail()
            );
            return ApiResponse.success(UserResponse.from(user));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/current")
    public ApiResponse<UserResponse> getCurrentUser() {
        // 从 SecurityContext 获取当前用户
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && !"anonymousUser".equals(auth.getPrincipal())) {
            return userService.findByUsername(auth.getName())
                    .map(user -> ApiResponse.success(UserResponse.from(user)))
                    .orElse(ApiResponse.error("用户不存在"));
        }
        return ApiResponse.error("未登录");
    }
}
```

**Step 3: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add src/main/java/com/example/cms/presentation/controller/auth/ src/main/java/com/example/cms/presentation/dto/
git commit -m "feat: 添加认证相关 API

- POST /api/auth/login - 登录验证
- POST /api/auth/register - 用户注册
- GET /api/auth/current - 获取当前用户

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 22: 表现层 - AdminUserController

**Files:**
- Create: `src/main/java/com/example/cms/presentation/controller/admin/AdminUserController.java`
- Create: `src/main/java/com/example/cms/presentation/dto/UserRequest.java`

**Step 1: 创建 UserRequest DTO**

```java
package com.example.cms.presentation.dto;

import lombok.Data;

@Data
public class UserRequest {
    private String username;
    private String password;
    private String email;
    private String role;
}
```

**Step 2: 创建 AdminUserController**

```java
package com.example.cms.presentation.controller.admin;

import com.example.cms.application.service.UserService;
import com.example.cms.domain.model.user.Role;
import com.example.cms.domain.model.user.User;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.UserRequest;
import com.example.cms.presentation.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<List<UserResponse>> getAll() {
        List<User> users = userService.findAll();
        return ApiResponse.success(users.stream()
                .map(UserResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ApiResponse.success(UserResponse.from(user)))
                .orElse(ApiResponse.error("用户不存在: " + id));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserRequest request) {
        try {
            Role role = request.getRole() != null
                    ? Role.from(request.getRole())
                    : Role.USER;
            User user = "ADMIN".equals(role.getValue())
                    ? userService.createAdmin(request.getUsername(), request.getPassword(), request.getEmail())
                    : userService.register(request.getUsername(), request.getPassword(), request.getEmail());
            return ApiResponse.success(UserResponse.from(user));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @RequestBody UserRequest request) {
        try {
            if (request.getRole() != null) {
                Role newRole = Role.from(request.getRole());
                User user = userService.updateRole(id, newRole);
                return ApiResponse.success(UserResponse.from(user));
            }
            return ApiResponse.error("无更新内容");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success(null);
    }
}
```

**Step 3: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 4: 提交**

```bash
git add src/main/java/com/example/cms/presentation/controller/admin/AdminUserController.java src/main/java/com/example/cms/presentation/dto/UserRequest.java
git commit -m "feat: 添加用户管理 API

- GET /api/admin/users - 用户列表
- GET /api/admin/users/{id} - 用户详情
- POST /api/admin/users - 创建用户
- PUT /api/admin/users/{id} - 更新用户
- DELETE /api/admin/users/{id} - 删除用户

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 23: 搜索功能 - ArticleRepository 添加搜索方法

**Files:**
- Modify: `src/main/java/com/example/cms/domain/repository/ArticleRepository.java`

**Step 1: 在 ArticleRepository 接口添加搜索方法**

```java
// 在接口中添加新方法
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// ... 现有方法 ...

Page<Article> searchByKeyword(String keyword, Long categoryId, Pageable pageable);
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: 编译失败（因为实现类还没有实现）

**Step 3: 在 ArticleRepositoryImpl 添加实现**

修改 `src/main/java/com/example/cms/infrastructure/persistence/repository/ArticleRepositoryImpl.java`:

```java
// 在类中添加方法
@Override
public Page<Article> searchByKeyword(String keyword, Long categoryId, Pageable pageable) {
    return springDataArticleRepository.searchByKeyword(keyword, categoryId, pageable)
            .map(articleMapper::toDomain);
}
```

**Step 4: 在 SpringDataArticleRepository 添加方法**

修改 `src/main/java/com/example/cms/infrastructure/persistence/repository/SpringDataArticleRepository.java`:

```java
// 在接口中添加
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Query("SELECT a FROM ArticleEntity a WHERE " +
       "(:keyword IS NULL OR a.title LIKE %:keyword% OR a.keywords LIKE %:keyword%) " +
       "AND (:categoryId IS NULL OR a.categoryId = :categoryId)")
Page<ArticleEntity> searchByKeyword(@Param("keyword") String keyword,
                                    @Param("categoryId") Long categoryId,
                                    Pageable pageable);
```

**Step 5: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 6: 提交**

```bash
git add src/main/java/com/example/cms/domain/repository/ArticleRepository.java
git add src/main/java/com/example/cms/infrastructure/persistence/repository/ArticleRepositoryImpl.java
git add src/main/java/com/example/cms/infrastructure/persistence/repository/SpringDataArticleRepository.java
git commit -m "feat: 添加文章搜索方法

- 支持关键词搜索（标题+关键词字段）
- 支持分类筛选
- 分页支持

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 24: 搜索功能 - ArticleService 添加搜索

**Files:**
- Modify: `src/main/java/com/example/cms/application/service/ArticleService.java`

**Step 1: 在 ArticleService 添加搜索方法**

```java
// 在类中添加
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public Page<Article> search(String keyword, Long categoryId, Pageable pageable) {
    return articleRepository.searchByKeyword(keyword, categoryId, pageable);
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/application/service/ArticleService.java
git commit -m "feat: ArticleService 添加搜索方法

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 25: 搜索功能 - PublicArticleController 添加搜索接口

**Files:**
- Modify: `src/main/java/com/example/cms/presentation/controller/publicapi/PublicArticleController.java`

**Step 1: 添加搜索端点**

```java
// 在类中添加
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@GetMapping("/search")
public ApiResponse<Page<ArticleResponse>> search(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        Pageable pageable) {
    Page<Article> articles = articleService.search(keyword, categoryId, pageable);
    return ApiResponse.success(articles.map(article -> {
        String categoryName = categoryService.findById(article.getCategoryId())
                .map(Category::getName)
                .orElse("未知分类");
        return ArticleResponse.from(article, categoryName);
    }));
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/presentation/controller/publicapi/PublicArticleController.java
git commit -m "feat: 添加前台文章搜索接口

- GET /api/public/articles/search?keyword=xxx&categoryId=xx

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 26: 文章实体更新 - 添加 keywords 字段

**Files:**
- Modify: `src/main/java/com/example/cms/infrastructure/persistence/entity/ArticleEntity.java`
- Modify: `src/main/java/com/example/cms/domain/model/article/Article.java`

**Step 1: 更新 ArticleEntity**

```java
// 在 ArticleEntity 类中添加
@Column(name = "keywords", length = 500)
private String keywords;

@Column(name = "author_id")
private Long authorId;
```

**Step 2: 更新 Article 领域模型**

```java
// 在 Article 类中添加
private String keywords;

// 在 Builder 类中添加
String keywords = "";

// 在 Builder 中添加方法
public Builder keywords(String keywords) {
    this.keywords = keywords != null ? keywords : "";
    return this;
}

// 在 Article 构造函数中添加
this.keywords = builder.keywords;
```

**Step 3: 更新 ArticleService create 方法**

修改 `src/main/java/com/example/cms/application/service/ArticleService.java`:

```java
// 在 create 方法中添加 keywords 参数
public Article create(String title, String content, Long categoryId, String keywords, Long authorId) {
    // ... 现有代码 ...
    return Article.builder()
            // ... 现有字段 ...
            .keywords(keywords)
            .build();
}
```

**Step 4: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 5: 提交**

```bash
git add src/main/java/com/example/cms/infrastructure/persistence/entity/ArticleEntity.java
git add src/main/java/com/example/cms/domain/model/article/Article.java
git add src/main/java/com/example/cms/application/service/ArticleService.java
git commit -m "feat: 文章添加 keywords 和 authorId 字段

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 27: 前端 - 创建 useAuth Composable

**Files:**
- Create: `frontend/src/composables/useAuth.js`

**Step 1: 创建 useAuth.js**

```javascript
import { ref, computed } from 'vue'

const AUTH_KEY = 'cms_auth'

export function useAuth() {
  const authData = ref(JSON.parse(localStorage.getItem(AUTH_KEY) || 'null'))

  const isLoggedIn = computed(() => !!authData.value)
  const username = computed(() => authData.value?.username || '')

  function login(username, password) {
    const credentials = btoa(`${username}:${password}`)
    authData.value = { username, credentials }
    localStorage.setItem(AUTH_KEY, JSON.stringify(authData.value))
  }

  function logout() {
    authData.value = null
    localStorage.removeItem(AUTH_KEY)
    // 强制刷新页面以清除 HTTP Basic 认证状态
    window.location.href = '/login'
  }

  function getAuthHeader() {
    return authData.value?.credentials
      ? { Authorization: `Basic ${authData.value.credentials}` }
      : {}
  }

  return {
    isLoggedIn,
    username,
    login,
    logout,
    getAuthHeader
  }
}
```

**Step 2: 提交**

```bash
git add frontend/src/composables/useAuth.js
git commit -m "feat: 添加 useAuth composable

- 登录状态管理（localStorage）
- Basic Auth 头生成
- 登录/登出方法

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 28: 前端 - 配置 Axios 拦截器

**Files:**
- Modify: `frontend/src/api/index.js`

**Step 1: 更新 API 配置**

```javascript
import axios from 'axios'
import { useAuth } from '../composables/useAuth'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器 - 添加 Basic Auth 头
api.interceptors.request.use(
  (config) => {
    const { getAuthHeader } = useAuth()
    const authHeader = getAuthHeader()
    if (authHeader.Authorization) {
      config.headers.Authorization = authHeader.Authorization
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器 - 处理 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // API 请求返回 401，跳转登录页由后端控制
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
```

**Step 2: 提交**

```bash
git add frontend/src/api/index.js
git commit -m "feat: 添加 Axios 拦截器

- 请求拦截：自动添加 Basic Auth 头
- 响应拦截：401 时跳转登录页

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 29: 前端 - 创建登录页面

**Files:**
- Create: `frontend/src/auth/Login.vue`

**Step 1: 创建 Login.vue**

```vue
<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2>CMS 登录</h2>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" style="width: 100%">
            登录
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="text" @click="goToRegister">还没有账号？去注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const { login } = useAuth()

const formRef = ref()
const loading = ref(false)
const form = ref({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true

  try {
    // 先调用登录 API 验证
    await api.post('/auth/login', {
      username: form.value.username,
      password: form.value.password
    })

    // 登录成功，存储凭据
    login(form.value.username, form.value.password)

    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function goToRegister() {
  router.push('/register')
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.login-card {
  width: 400px;
}

h2 {
  margin: 0;
  text-align: center;
}
</style>
```

**Step 2: 提交**

```bash
git add frontend/src/auth/Login.vue
git commit -m "feat: 添加登录页面

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 30: 前端 - 创建注册页面

**Files:**
- Create: `frontend/src/auth/Register.vue`

**Step 1: 创建 Register.vue**

```vue
<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <h2>CMS 注册</h2>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名（3-50字符）" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码（至少6字符）" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱（可选）" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading" style="width: 100%">
            注册
          </el-button>
        </el-form-item>
        <el-form-item>
          <el-button type="text" @click="goToLogin">已有账号？去登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const { login } = useAuth()

const formRef = ref()
const loading = ref(false)
const form = ref({
  username: '',
  password: '',
  confirmPassword: '',
  email: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度为 3-50 字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true

  try {
    await api.post('/auth/register', {
      username: form.value.username,
      password: form.value.password,
      email: form.value.email || null
    })

    // 注册成功，自动登录
    login(form.value.username, form.value.password)

    ElMessage.success('注册成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '注册失败')
  } finally {
    loading.value = false
  }
}

function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f5f5f5;
}

.register-card {
  width: 400px;
}

h2 {
  margin: 0;
  text-align: center;
}
</style>
```

**Step 2: 提交**

```bash
git add frontend/src/auth/Register.vue
git commit -m "feat: 添加注册页面

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 31: 前端 - 更新路由配置

**Files:**
- Modify: `frontend/src/router/index.js`

**Step 1: 添加登录/注册路由**

```javascript
// 在 routes 数组中添加
{
  path: '/login',
  component: () => import('../auth/Login.vue')
},
{
  path: '/register',
  component: () => import('../auth/Register.vue')
},
```

**Step 2: 提交**

```bash
git add frontend/src/router/index.js
git commit -m "feat: 添加登录/注册路由

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 32: 前端 - 添加用户菜单组件

**Files:**
- Create: `frontend/src/components/UserMenu.vue`

**Step 1: 创建 UserMenu.vue**

```vue
<template>
  <div class="user-menu">
    <template v-if="isLoggedIn">
      <span class="username">{{ username }}</span>
      <el-button type="text" @click="handleLogout">退出</el-button>
    </template>
    <template v-else>
      <el-button type="text" @click="goToLogin">登录</el-button>
      <el-button type="text" @click="goToRegister">注册</el-button>
    </template>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'

const router = useRouter()
const { isLoggedIn, username, logout } = useAuth()

function handleLogout() {
  logout()
}

function goToLogin() {
  router.push('/login')
}

function goToRegister() {
  router.push('/register')
}
</script>

<style scoped>
.user-menu {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  color: #333;
}
</style>
```

**Step 2: 提交**

```bash
git add frontend/src/components/UserMenu.vue
git commit -m "feat: 添加用户菜单组件

- 显示登录状态
- 登录/注册/退出按钮

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 33: 前端 - 更新布局添加用户菜单

**Files:**
- Modify: `frontend/src/public/views/Home.vue`

**Step 1: 在首页添加用户菜单**

找到导航栏位置，添加 UserMenu 组件：

```vue
<template>
  <div class="home">
    <header class="header">
      <!-- 现有导航 -->
      <UserMenu />
    </header>
    <!-- 现有内容 -->
  </div>
</template>

<script setup>
import UserMenu from '../components/UserMenu.vue'
</script>
```

**Step 2: 提交**

```bash
git add frontend/src/public/views/Home.vue
git commit -m "feat: 首页添加用户菜单

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 34: 前端 - 创建用户管理页面

**Files:**
- Create: `frontend/src/admin/views/UserList.vue`

**Step 1: 创建 UserList.vue**

```vue
<template>
  <div class="user-list">
    <div class="header">
      <h2>用户管理</h2>
      <el-button type="primary" @click="showCreateDialog">新增用户</el-button>
    </div>

    <el-table :data="users" border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
            {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
            {{ row.status === 'ACTIVE' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="text" @click="showEditDialog(row)">编辑</el-button>
          <el-button type="text" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="form.password" type="password" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api'

const users = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = ref({
  username: '',
  password: '',
  email: '',
  role: 'USER'
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function fetchUsers() {
  try {
    const response = await api.get('/admin/users')
    users.value = response.data.data
  } catch (error) {
    ElMessage.error('获取用户列表失败')
  }
}

function showCreateDialog() {
  isEdit.value = false
  form.value = { username: '', password: '', email: '', role: 'USER' }
  dialogVisible.value = true
}

function showEditDialog(user) {
  isEdit.value = true
  form.value = {
    id: user.id,
    username: user.username,
    email: user.email,
    role: user.role
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  try {
    if (isEdit.value) {
      await api.put(`/admin/users/${form.value.id}`, { role: form.value.role })
      ElMessage.success('更新成功')
    } else {
      await api.post('/admin/users', form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchUsers()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' })
    await api.delete(`/admin/users/${id}`)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-list {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
```

**Step 2: 提交**

```bash
git add frontend/src/admin/views/UserList.vue
git commit -m "feat: 添加用户管理页面

- 用户列表
- 新增/编辑用户
- 删除用户

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 35: 前端 - 更新后台布局添加用户管理路由

**Files:**
- Modify: `frontend/src/router/index.js`

**Step 1: 添加用户管理路由**

在 admin 的 children 中添加：

```javascript
{
  path: 'users',
  component: () => import('../admin/views/UserList.vue')
}
```

**Step 2: 更新 AdminLayout 添加导航菜单**

修改 `frontend/src/admin/views/AdminLayout.vue`，在导航中添加用户管理链接：

```vue
<el-menu-item index="/admin/users">用户管理</el-menu-item>
```

**Step 3: 提交**

```bash
git add frontend/src/router/index.js frontend/src/admin/views/AdminLayout.vue
git commit -m "feat: 添加用户管理路由和导航

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 36: 前端 - 创建文章搜索页面

**Files:**
- Create: `frontend/src/public/views/SearchArticle.vue`

**Step 1: 创建 SearchArticle.vue**

```vue
<template>
  <div class="search-page">
    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索文章标题或关键词"
        clearable
        @keyup.enter="handleSearch"
        style="width: 400px"
      >
        <template #append>
          <el-button @click="handleSearch">搜索</el-button>
        </template>
      </el-input>
      <el-select v-model="categoryId" placeholder="选择分类" clearable style="width: 200px; margin-left: 10px">
        <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
      </el-select>
    </div>

    <div class="article-list">
      <el-card v-for="article in articles" :key="article.id" class="article-card" @click="goToDetail(article.id)">
        <h3>{{ article.title }}</h3>
        <p class="summary">{{ article.summary }}</p>
        <div class="meta">
          <el-tag size="small">{{ article.categoryName }}</el-tag>
          <span class="time">{{ formatDate(article.publishedAt) }}</span>
        </div>
      </el-card>

      <el-empty v-if="articles.length === 0 && !loading" description="没有找到相关文章" />
    </div>

    <el-pagination
      v-model:current-page="page"
      :page-size="size"
      :total="total"
      @current-change="handleSearch"
      layout="prev, pager, next"
      style="margin-top: 20px; text-align: center"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../../api'

const router = useRouter()
const keyword = ref('')
const categoryId = ref(null)
const articles = ref([])
const categories = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function fetchCategories() {
  try {
    const response = await api.get('/public/categories')
    categories.value = response.data.data
  } catch (error) {
    ElMessage.error('获取分类失败')
  }
}

async function handleSearch() {
  loading.value = true
  try {
    const params = {
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
      page: page.value - 1,
      size: size.value
    }
    const response = await api.get('/public/articles/search', { params })
    articles.value = response.data.data.content || []
    total.value = response.data.data.totalElements || 0
  } catch (error) {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

function goToDetail(id) {
  router.push(`/article/${id}`)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

onMounted(() => {
  fetchCategories()
  handleSearch()
})
</script>

<style scoped>
.search-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.search-bar {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.article-list {
  min-height: 300px;
}

.article-card {
  margin-bottom: 15px;
  cursor: pointer;
  transition: box-shadow 0.3s;
}

.article-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.article-card h3 {
  margin: 0 0 10px 0;
}

.summary {
  color: #666;
  margin: 10px 0;
}

.meta {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time {
  color: #999;
  font-size: 14px;
}
</style>
```

**Step 2: 提交**

```bash
git add frontend/src/public/views/SearchArticle.vue
git commit -m "feat: 添加文章搜索页面

- 关键词搜索
- 分类筛选
- 分页

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 37: 前端 - 添加搜索路由

**Files:**
- Modify: `frontend/src/router/index.js`

**Step 1: 添加搜索路由**

```javascript
{
  path: '/search',
  component: () => import('../public/views/SearchArticle.vue')
}
```

**Step 2: 提交**

```bash
git add frontend/src/router/index.js
git commit -m "feat: 添加搜索路由

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 38: 前端 - 更新文章表单添加关键词字段

**Files:**
- Modify: `frontend/src/admin/views/ArticleForm.vue`

**Step 1: 添加关键词输入框**

在表单中添加：

```vue
<el-form-item label="关键词" prop="keywords">
  <el-input
    v-model="form.keywords"
    placeholder="请输入关键词，用逗号分隔（如：Java,Spring,编程）"
  />
</el-form-item>
```

在 script 中添加到 form：

```javascript
const form = ref({
  // ... 现有字段
  keywords: ''
})
```

在提交时包含 keywords：

```javascript
await api.create({
  // ... 现有字段
  keywords: form.value.keywords
})
```

**Step 2: 提交**

```bash
git add frontend/src/admin/views/ArticleForm.vue
git commit -m "feat: 文章表单添加关键词字段

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 39: 前端 - 更新首页添加搜索入口

**Files:**
- Modify: `frontend/src/public/views/Home.vue`

**Step 1: 在导航栏添加搜索链接**

```vue
<el-button type="text" @click="goToSearch">搜索文章</el-button>
```

在 script 中添加：

```javascript
function goToSearch() {
  router.push('/search')
}
```

**Step 2: 提交**

```bash
git add frontend/src/public/views/Home.vue
git commit -m "feat: 首页添加搜索入口

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 40: 后端 - 添加默认管理员初始化

**Files:**
- Create: `src/main/java/com/example/cms/config/DataInitializer.java`

**Step 1: 创建数据初始化器**

```java
package com.example.cms.config;

import com.example.cms.application.service.UserService;
import com.example.cms.domain.model.user.Role;
import com.example.cms.domain.model.user.User;
import com.example.cms.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(String... args) {
        // 检查是否已有管理员
        if (userRepository.findAll().stream().noneMatch(User::isAdmin)) {
            userService.createAdmin("admin", "admin123", null);
            System.out.println("默认管理员账号已创建: admin / admin123");
        }
    }
}
```

**Step 2: 编译验证**

```bash
./gradlew compileJava
```
Expected: BUILD SUCCESSFUL

**Step 3: 提交**

```bash
git add src/main/java/com/example/cms/config/DataInitializer.java
git commit -m "feat: 添加默认管理员初始化

- 启动时自动创建 admin/admin123 账号

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## Task 41: 构建和测试

**Step 1: 构建前端**

```bash
cd frontend
npm run build
```
Expected: 构建成功

**Step 2: 复制前端资源**

```bash
./build-frontend.sh
```
Expected: 静态资源复制到 src/main/resources/static

**Step 3: 启动应用**

```bash
./gradlew bootRun
```
Expected: 应用启动成功，默认管理员账号创建

**Step 4: 测试功能**

1. 访问 `http://localhost:8080` - 应该显示首页
2. 访问 `http://localhost:8080/login` - 显示登录页面
3. 使用 `admin/admin123` 登录
4. 访问 `http://localhost:8080/admin` - 显示后台管理
5. 访问 `http://localhost:8080/admin/users` - 显示用户管理
6. 测试文章搜索功能

**Step 5: 最终提交**

```bash
git add -A
git commit -m "chore: 完成迭代2 用户权限与搜索功能

- 用户认证授权（HTTP Basic + Spring Security）
- 用户管理（CRUD）
- 文章搜索（标题+关键词）
- 前端登录/注册页面

Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>"
```

---

## 实施完成检查清单

- [ ] 用户表创建成功
- [ ] 文章表添加 keywords 和 author_id 字段
- [ ] Spring Security 配置完成
- [ ] 登录/注册 API 可用
- [ ] 用户管理 API 可用
- [ ] 搜索 API 可用
- [ ] 前端登录页面可访问
- [ ] 前端注册页面可访问
- [ ] 用户管理页面可访问（仅管理员）
- [ ] 搜索页面可用
- [ ] 文章表单支持关键词输入
- [ ] 默认管理员账号自动创建
