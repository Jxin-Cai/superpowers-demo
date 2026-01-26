# CMS MVP 实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 构建一个最小可用的内容管理系统，包含前台展示和后台管理功能。

**架构：** 前后端一体化部署，Spring Boot 提供 REST API 和静态文件服务，前端采用 Vue 3 + Element Plus，后端遵循 DDD 分层架构。

**技术栈：**
- 后端：Java 17 + Spring Boot 3.x + Spring Data JPA + H2 + Flyway + flexmark-java
- 前端：Vue 3 + Element Plus + Vue Router + Axios
- 构建：Gradle (后端) + npm (前端)

---

## 任务概览

1. 后端项目初始化（Spring Boot + Gradle）
2. 数据库初始化（Flyway 迁移脚本）
3. 领域层实现（Entity + Value Object + Repository 接口）
4. 基础设施层实现（JPA Repository + Markdown 渲染器）
5. 应用层实现（Service）
6. 展示层实现（Controller + DTO）
7. 前端项目初始化（Vue 3 + Element Plus）
8. 后台管理页面实现
9. 前台展示页面实现
10. 前后端集成

---

## Task 1: 后端项目初始化

**目标：** 创建 Spring Boot 项目基础结构和 Gradle 配置。

**文件：**
- 创建：`build.gradle`
- 创建：`settings.gradle`
- 创建：`src/main/resources/application.yml`
- 创建：`src/main/java/com/example/cms/CmsApplication.java`

### Step 1: 创建 build.gradle

创建 `build.gradle` 文件：

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.1'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Flyway
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'

    // H2 Database
    runtimeOnly 'com.h2database:h2'

    // Markdown Renderer
    implementation 'com.vladsch.flexmark:flexmark-all:0.64.8'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

### Step 2: 创建 settings.gradle

创建 `settings.gradle` 文件：

```groovy
rootProject.name = 'cms-mvp'
```

### Step 3: 创建 application.yml

创建 `src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: cms-mvp

  datasource:
    url: jdbc:h2:mem:cmsdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  flyway:
    enabled: true
    baseline-on-migrate: true

server:
  port: 8080
```

### Step 4: 创建主应用类

创建 `src/main/java/com/example/cms/CmsApplication.java`：

```java
package com.example.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CmsApplication.class, args);
    }
}
```

### Step 5: 验证编译

运行：`./gradlew build`
预期输出：BUILD SUCCESSFUL

### Step 6: Commit

```bash
git add build.gradle settings.gradle src/
git commit -m "feat: 初始化 Spring Boot 项目结构"
```

---

## Task 2: 数据库初始化

**目标：** 创建 Flyway 迁移脚本，定义数据库表结构。

**文件：**
- 创建：`src/main/resources/db/migration/V1__Create_categories_table.sql`
- 创建：`src/main/resources/db/migration/V2__Create_articles_table.sql`

### Step 1: 创建 categories 表迁移脚本

创建 `src/main/resources/db/migration/V1__Create_categories_table.sql`：

```sql
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_categories_name ON categories(name);
```

### Step 2: 创建 articles 表迁移脚本

创建 `src/main/resources/db/migration/V2__Create_articles_table.sql`：

```sql
CREATE TABLE articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content CLOB NOT NULL,
    rendered_content CLOB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    category_id BIGINT NOT NULL,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_articles_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
);

CREATE INDEX idx_articles_status ON articles(status);
CREATE INDEX idx_articles_category ON articles(category_id);
CREATE INDEX idx_articles_published_at ON articles(published_at);
```

### Step 3: 验证迁移

运行：`./gradlew bootRun`
预期输出：日志显示 Flyway 迁移成功

### Step 4: Commit

```bash
git add src/main/resources/db/
git commit -m "feat: 添加数据库迁移脚本"
```

---

## Task 3: 领域层 - Article 聚合

**目标：** 实现 Article 领域模型（聚合根、值对象）。

**文件：**
- 创建：`src/main/java/com/example/cms/domain/model/article/Article.java`
- 创建：`src/main/java/com/example/cms/domain/model/article/ArticleStatus.java`
- 创建：`src/main/java/com/example/cms/domain/model/article/RenderedContent.java`

### Step 1: 创建 ArticleStatus 枚举

创建 `src/main/java/com/example/cms/domain/model/article/ArticleStatus.java`：

```java
package com.example.cms.domain.model.article;

import lombok.Getter;

@Getter
public enum ArticleStatus {
    DRAFT("草稿"),
    PUBLISHED("已发布");

    private final String description;

    ArticleStatus(String description) {
        this.description = description;
    }
}
```

### Step 2: 创建 RenderedContent 值对象

创建 `src/main/java/com/example/cms/domain/model/article/RenderedContent.java`：

```java
package com.example.cms.domain.model.article;

import lombok.Value;

@Value
public class RenderedContent {
    String html;

    public static RenderedContent of(String html) {
        if (html == null || html.isBlank()) {
            return new RenderedContent("");
        }
        return new RenderedContent(html);
    }
}
```

### Step 3: 创建 Article 聚合根

创建 `src/main/java/com/example/cms/domain/model/article/Article.java`：

```java
package com.example.cms.domain.model.article;

import com.example.cms.domain.shared.Audit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDateTime;

@Getter
public class Article {
    private final Long id;
    private String title;
    private String content;
    private RenderedContent renderedContent;
    private ArticleStatus status;
    private final Long categoryId;
    private LocalDateTime publishedAt;
    private final Audit audit;

    private Article(Builder builder) {
        this.id = builder.id;
        this.title = builder.title;
        this.content = builder.content;
        this.renderedContent = builder.renderedContent;
        this.status = builder.status;
        this.categoryId = builder.categoryId;
        this.publishedAt = builder.publishedAt;
        this.audit = builder.audit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void updateContent(String content, RenderedContent renderedContent) {
        this.content = content;
        this.renderedContent = renderedContent;
        this.audit.markModified();
    }

    public void publish() {
        if (this.status == ArticleStatus.PUBLISHED) {
            return;
        }
        this.status = ArticleStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
        this.audit.markModified();
    }

    public void unpublish() {
        if (this.status == ArticleStatus.DRAFT) {
            return;
        }
        this.status = ArticleStatus.DRAFT;
        this.publishedAt = null;
        this.audit.markModified();
    }

    public void changeCategory(Long categoryId) {
        this.categoryId = categoryId;
        this.audit.markModified();
    }

    @Value
    public static class Builder {
        Long id;
        String title;
        String content;
        RenderedContent renderedContent = RenderedContent.of("");
        ArticleStatus status = ArticleStatus.DRAFT;
        Long categoryId;
        LocalDateTime publishedAt;
        Audit audit = Audit.create();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder renderedContent(RenderedContent renderedContent) {
            this.renderedContent = renderedContent;
            return this;
        }

        public Builder status(ArticleStatus status) {
            this.status = status;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public Builder audit(Audit audit) {
            this.audit = audit;
            return this;
        }

        public Article build() {
            return new Article(this);
        }
    }
}
```

### Step 4: 创建 Audit 值对象

创建 `src/main/java/com/example/cms/domain/shared/Audit.java`：

```java
package com.example.cms.domain.shared;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class Audit {
    LocalDateTime createdAt;
    @Getter(AccessLevel.NONE)
    LocalDateTime updatedAt;

    private Audit(LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Audit create() {
        LocalDateTime now = LocalDateTime.now();
        return new Audit(now, now);
    }

    public static Audit of(LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Audit(createdAt, updatedAt);
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt != null ? updatedAt : createdAt;
    }

    public Audit markModified() {
        return new Audit(this.createdAt, LocalDateTime.now());
    }
}
```

### Step 5: 验证编译

运行：`./gradlew compileJava`
预期输出：BUILD SUCCESSFUL

### Step 6: Commit

```bash
git add src/main/java/com/example/cms/domain/
git commit -m "feat: 实现 Article 领域模型"
```

---

## Task 4: 领域层 - Category 聚合

**目标：** 实现 Category 领域模型。

**文件：**
- 创建：`src/main/java/com/example/cms/domain/model/category/Category.java`

### Step 1: 创建 Category 聚合根

创建 `src/main/java/com/example/cms/domain/model/category/Category.java`：

```java
package com.example.cms.domain.model.category;

import com.example.cms.domain.shared.Audit;
import lombok.Getter;

import java.util.Optional;

@Getter
public class Category {
    private final Long id;
    private String name;
    private String description;
    private final Audit audit;

    private Category(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.description = builder.description;
        this.audit = builder.audit;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void rename(String name) {
        this.name = name;
        this.audit.markModified();
    }

    public void updateDescription(String description) {
        this.description = description;
        this.audit.markModified();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private Audit audit = Audit.create();

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder audit(Audit audit) {
            this.audit = audit;
            return this;
        }

        public Category build() {
            return new Category(this);
        }
    }
}
```

### Step 2: 验证编译

运行：`./gradlew compileJava`
预期输出：BUILD SUCCESSFUL

### Step 3: Commit

```bash
git add src/main/java/com/example/cms/domain/model/category/
git commit -m "feat: 实现 Category 领域模型"
```

---

## Task 5: 领域层 - Repository 接口

**目标：** 定义领域层 Repository 接口。

**文件：**
- 创建：`src/main/java/com/example/cms/domain/repository/ArticleRepository.java`
- 创建：`src/main/java/com/example/cms/domain/repository/CategoryRepository.java`

### Step 1: 创建 ArticleRepository 接口

创建 `src/main/java/com/example/cms/domain/repository/ArticleRepository.java`：

```java
package com.example.cms.domain.repository;

import com.example.cms.domain.model.article.Article;
import com.example.cms.domain.model.article.ArticleStatus;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository {
    Article save(Article article);
    Optional<Article> findById(Long id);
    List<Article> findAll();
    List<Article> findByCategoryId(Long categoryId);
    List<Article> findByStatus(ArticleStatus status);
    List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status);
    List<Article> findByCategoryIdAndStatus(Long categoryId, ArticleStatus status);
    void deleteById(Long id);
    boolean existsByCategoryId(Long categoryId);
}
```

### Step 2: 创建 CategoryRepository 接口

创建 `src/main/java/com/example/cms/domain/repository/CategoryRepository.java`：

```java
package com.example.cms.domain.repository;

import com.example.cms.domain.model.category.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(Long id);
    List<Category> findAll();
    void deleteById(Long id);
    boolean existsByName(String name);
}
```

### Step 3: 验证编译

运行：`./gradlew compileJava`
预期输出：BUILD SUCCESSFUL

### Step 4: Commit

```bash
git add src/main/java/com/example/cms/domain/repository/
git commit -m "feat: 定义领域层 Repository 接口"
```

---

## Task 6: 基础设施层 - JPA Entity

**目标：** 实现 JPA Entity（用于持久化）。

**文件：**
- 创建：`src/main/java/com/example/cms/infrastructure/persistence/entity/CategoryEntity.java`
- 创建：`src/main/java/com/example/cms/infrastructure/persistence/entity/ArticleEntity.java`

### Step 1: 创建 CategoryEntity

创建 `src/main/java/com/example/cms/infrastructure/persistence/entity/CategoryEntity.java`：

```java
package com.example.cms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static CategoryEntity of(String name, String description) {
        CategoryEntity entity = new CategoryEntity();
        entity.name = name;
        entity.description = description;
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = LocalDateTime.now();
        return entity;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### Step 2: 创建 ArticleEntity

创建 `src/main/java/com/example/cms/infrastructure/persistence/entity/ArticleEntity.java`：

```java
package com.example.cms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String content;

    @Column(nullable = false, columnDefinition = "CLOB")
    private String renderedContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static ArticleEntity of(String title, String content, String renderedContent,
                                    String status, Long categoryId) {
        ArticleEntity entity = new ArticleEntity();
        entity.title = title;
        entity.content = content;
        entity.renderedContent = renderedContent;
        entity.status = status;
        entity.categoryId = categoryId;
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = LocalDateTime.now();
        return entity;
    }

    public void update(String title, String content, String renderedContent, Long categoryId) {
        this.title = title;
        this.content = content;
        this.renderedContent = renderedContent;
        this.categoryId = categoryId;
        this.updatedAt = LocalDateTime.now();
    }

    public void publish() {
        this.status = "PUBLISHED";
        this.publishedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void unpublish() {
        this.status = "DRAFT";
        this.publishedAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
```

### Step 3: 验证编译

运行：`./gradlew compileJava`
预期输出：BUILD SUCCESSFUL

### Step 4: Commit

```bash
git add src/main/java/com/example/cms/infrastructure/persistence/entity/
git commit -m "feat: 实现 JPA Entity"
```

---

## Task 7: 基础设施层 - Repository 实现

**目标：** 实现 JPA Repository。

**文件：**
- 创建：`src/main/java/com/example/cms/infrastructure/persistence/repository/SpringDataCategoryRepository.java`
- 创建：`src/main/java/com/example/cms/infrastructure/persistence/repository/SpringDataArticleRepository.java`
- 创建：`src/main/java/com/example/cms/infrastructure/persistence/repository/CategoryRepositoryImpl.java`
- 创建：`src/main/java/com/example/cms/infrastructure/persistence/repository/ArticleRepositoryImpl.java`

### Step 1: 创建 SpringDataCategoryRepository

创建 `src/main/java/com/example/cms/infrastructure/persistence/repository/SpringDataCategoryRepository.java`：

```java
package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.infrastructure.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataCategoryRepository extends JpaRepository<CategoryEntity, Long> {
    Optional<CategoryEntity> findByName(String name);
    boolean existsByName(String name);
}
```

### Step 2: 创建 SpringDataArticleRepository

创建 `src/main/java/com/example/cms/infrastructure/persistence/repository/SpringDataArticleRepository.java`：

```java
package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.infrastructure.persistence.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataArticleRepository extends JpaRepository<ArticleEntity, Long> {
    List<ArticleEntity> findByCategoryId(Long categoryId);
    List<ArticleEntity> findByStatus(String status);
    List<ArticleEntity> findByStatusOrderByPublishedAtDesc(String status);
    List<ArticleEntity> findByCategoryIdAndStatus(Long categoryId, String status);
    boolean existsByCategoryId(Long categoryId);
}
```

### Step 3: 创建 CategoryRepositoryImpl

创建 `src/main/java/com/example/cms/infrastructure/persistence/repository/CategoryRepositoryImpl.java`：

```java
package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.domain.model.category.Category;
import com.example.cms.domain.repository.CategoryRepository;
import com.example.cms.infrastructure.persistence.entity.CategoryEntity;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final SpringDataCategoryRepository springDataRepository;

    @Override
    public Category save(Category category) {
        CategoryEntity entity = toEntity(category);
        CategoryEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return springDataRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return springDataRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return springDataRepository.existsByName(name);
    }

    private Category toDomain(CategoryEntity entity) {
        return Category.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .audit(com.example.cms.domain.shared.Audit.of(
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()
                ))
                .build();
    }

    private CategoryEntity toEntity(Category category) {
        if (category.getId() == null) {
            return CategoryEntity.of(category.getName(), category.getDescription());
        }
        CategoryEntity entity = new CategoryEntity();
        entity.update(category.getName(), category.getDescription());
        return entity;
    }
}
```

### Step 4: 创建 ArticleRepositoryImpl

创建 `src/main/java/com/example/cms/infrastructure/persistence/repository/ArticleRepositoryImpl.java`：

```java
package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.domain.model.article.Article;
import com.example.cms.domain.model.article.ArticleStatus;
import com.example.cms.domain.repository.ArticleRepository;
import com.example.cms.infrastructure.persistence.entity.ArticleEntity;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepository {

    private final SpringDataArticleRepository springDataRepository;

    @Override
    public Article save(Article article) {
        ArticleEntity entity = toEntity(article);
        ArticleEntity saved = springDataRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Article> findById(Long id) {
        return springDataRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Article> findAll() {
        return springDataRepository.findAll().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Article> findByCategoryId(Long categoryId) {
        return springDataRepository.findByCategoryId(categoryId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Article> findByStatus(ArticleStatus status) {
        return springDataRepository.findByStatus(status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status) {
        return springDataRepository.findByStatusOrderByPublishedAtDesc(status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Article> findByCategoryIdAndStatus(Long categoryId, ArticleStatus status) {
        return springDataRepository.findByCategoryIdAndStatus(categoryId, status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        springDataRepository.deleteById(id);
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        return springDataRepository.existsByCategoryId(categoryId);
    }

    private Article toDomain(ArticleEntity entity) {
        return Article.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .renderedContent(com.example.cms.domain.model.article.RenderedContent.of(entity.getRenderedContent()))
                .status(ArticleStatus.valueOf(entity.getStatus()))
                .categoryId(entity.getCategoryId())
                .publishedAt(entity.getPublishedAt())
                .audit(com.example.cms.domain.shared.Audit.of(
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()
                ))
                .build();
    }

    private ArticleEntity toEntity(Article article) {
        if (article.getId() == null) {
            return ArticleEntity.of(
                    article.getTitle(),
                    article.getContent(),
                    article.getRenderedContent().getHtml(),
                    article.getStatus().name(),
                    article.getCategoryId()
            );
        }
        ArticleEntity entity = new ArticleEntity();
        entity.update(
                article.getTitle(),
                article.getContent(),
                article.getRenderedContent().getHtml(),
                article.getCategoryId()
        );
        return entity;
    }
}
```

### Step 5: 验证编译

运行：`./gradlew compileJava`
预期输出：BUILD SUCCESSFUL

### Step 6: Commit

```bash
git add src/main/java/com/example/cms/infrastructure/persistence/repository/
git commit -m "feat: 实现 JPA Repository"
```

---

## Task 8: 领域服务 - Markdown 渲染器

**目标：** 实现 Markdown 到 HTML 的渲染服务。

**文件：**
- 创建：`src/main/java/com/example/cms/domain/service/MarkdownRenderer.java`

### Step 1: 创建 MarkdownRenderer

创建 `src/main/java/com/example/cms/domain/service/MarkdownRenderer.java`：

```java
package com.example.cms.domain.service;

import com.example.cms.domain.model.article.RenderedContent;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import org.springframework.stereotype.Service;

@Service
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        this.parser = Parser.builder().build();
        this.renderer = HtmlRenderer.builder().build();
    }

    public RenderedContent render(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return RenderedContent.of("");
        }

        Document document = parser.parse(markdown);
        String html = renderer.render(document);

        // 基本的 HTML 清理
        html = sanitizeHtml(html);

        return RenderedContent.of(html);
    }

    private String sanitizeHtml(String html) {
        // 移除潜在危险的标签和属性（简化版）
        html = html.replaceAll("<script[^>]*>.*?</script>", "");
        html = html.replaceAll("on\\w+\\s*=\\s*[\"'][^\"']*[\"']", "");
        return html;
    }
}
```

### Step 2: 验证编译

运行：`./gradlew compileJava`
预期输出：BUILD SUCCESSFUL

### Step 3: Commit

```bash
git add src/main/java/com/example/cms/domain/service/MarkdownRenderer.java
git commit -m "feat: 实现 Markdown 渲染器"
```

---

## Task 9: 应用层 - Category Service

**目标：** 实现分类应用服务。

**文件：**
- 创建：`src/main/java/com/example/cms/application/service/CategoryService.java`

### Step 1: 创建 CategoryService

创建 `src/main/java/com/example/cms/application/service/CategoryService.java`：

```java
package com.example.cms.application.service;

import com.example.cms.domain.model.category.Category;
import com.example.cms.domain.repository.CategoryRepository;
import com.example.cms.domain.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;

    @Transactional
    public Category create(String name, String description) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("分类名称已存在: " + name);
        }
        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();
        return categoryRepository.save(category);
    }

    @Transactional
    public Category update(Long id, String name, String description) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + id));

        if (!category.getName().equals(name) && categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("分类名称已存在: " + name);
        }

        category.rename(name);
        if (description != null) {
            category.updateDescription(description);
        }
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id) {
        if (articleRepository.existsByCategoryId(id)) {
            throw new IllegalArgumentException("该分类下有文章，无法删除");
        }
        categoryRepository.deleteById(id);
    }

    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }
}
```

### Step 2: 验证编译

运行：`./gradlew compileJava`
预期输出：BUILD SUCCESSFUL

### Step 3: Commit

```bash
git add src/main/java/com/example/cms/application/service/CategoryService.java
git commit -m "feat: 实现分类应用服务"
```

---

## Task 10: 应用层 - Article Service

**目标：** 实现文章应用服务。

**文件：**
- 创建：`src/main/java/com/example/cms/application/service/ArticleService.java`

### Step 1: 创建 ArticleService

创建 `src/main/java/com/example/cms/application/service/ArticleService.java`：

```java
package com.example.cms.application.service;

import com.example.cms.domain.model.article.Article;
import com.example.cms.domain.model.article.ArticleStatus;
import com.example.cms.domain.model.article.RenderedContent;
import com.example.cms.domain.repository.ArticleRepository;
import com.example.cms.domain.repository.CategoryRepository;
import com.example.cms.domain.service.MarkdownRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final MarkdownRenderer markdownRenderer;

    @Transactional
    public Article create(String title, String content, Long categoryId) {
        validateCategoryExists(categoryId);

        RenderedContent renderedContent = markdownRenderer.render(content);

        Article article = Article.builder()
                .title(title)
                .content(content)
                .renderedContent(renderedContent)
                .categoryId(categoryId)
                .build();
        return articleRepository.save(article);
    }

    @Transactional
    public Article update(Long id, String title, String content, Long categoryId) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在: " + id));

        validateCategoryExists(categoryId);

        RenderedContent renderedContent = markdownRenderer.render(content);
        article.updateContent(content, renderedContent);
        article.changeCategory(categoryId);

        return articleRepository.save(article);
    }

    @Transactional
    public void delete(Long id) {
        articleRepository.deleteById(id);
    }

    @Transactional
    public Article publish(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在: " + id));
        article.publish();
        return articleRepository.save(article);
    }

    @Transactional
    public Article unpublish(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("文章不存在: " + id));
        article.unpublish();
        return articleRepository.save(article);
    }

    public Optional<Article> findById(Long id) {
        return articleRepository.findById(id);
    }

    public List<Article> findAll() {
        return articleRepository.findAll();
    }

    public List<Article> findPublished() {
        return articleRepository.findByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED);
    }

    public List<Article> findByCategory(Long categoryId) {
        return articleRepository.findByCategoryIdAndStatus(categoryId, ArticleStatus.PUBLISHED);
    }

    private void validateCategoryExists(Long categoryId) {
        if (!categoryRepository.findById(categoryId).isPresent()) {
            throw new IllegalArgumentException("分类不存在: " + categoryId);
        }
    }
}
```

### Step 2: 验证编译

运行：`./gradlew compileJava`
预期输出：BUILD SUCCESSFUL

### Step 3: Commit

```bash
git add src/main/java/com/example/cms/application/service/ArticleService.java
git commit -m "feat: 实现文章应用服务"
```

---

## Task 11: 展示层 - DTO

**目标：** 创建请求和响应 DTO。

**文件：**
- 创建：`src/main/java/com/example/cms/presentation/dto/ApiResponse.java`
- 创建：`src/main/java/com/example/cms/presentation/dto/CategoryRequest.java`
- 创建：`src/main/java/com/example/cms/presentation/dto/CategoryResponse.java`
- 创建：`src/main/java/com/example/cms/presentation/dto/ArticleRequest.java`
- 创建：`src/main/java/com/example/cms/presentation/dto/ArticleResponse.java`

### Step 1: 创建 ApiResponse

创建 `src/main/java/com/example/cms/presentation/dto/ApiResponse.java`：

```java
package com.example.cms.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
```

### Step 2: 创建 CategoryRequest

创建 `src/main/java/com/example/cms/presentation/dto/CategoryRequest.java`：

```java
package com.example.cms.presentation.dto;

import lombok.Data;

@Data
public class CategoryRequest {
    private String name;
    private String description;
}
```

### Step 3: 创建 CategoryResponse

创建 `src/main/java/com/example/cms/presentation/dto/CategoryResponse.java`：

```java
package com.example.cms.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CategoryResponse from(com.example.cms.domain.model.category.Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getAudit().getCreatedAt(),
                category.getAudit().getUpdatedAt()
        );
    }
}
```

### Step 4: 创建 ArticleRequest

创建 `src/main/java/com/example/cms/presentation/dto/ArticleRequest.java`：

```java
package com.example.cms.presentation.dto;

import lombok.Data;

@Data
public class ArticleRequest {
    private String title;
    private String content;
    private Long categoryId;
}
```

### Step 5: 创建 ArticleResponse

创建 `src/main/java/com/example/cms/presentation/dto/ArticleResponse.java`：

```java
package com.example.cms.presentation.dto;

import com.example.cms.domain.model.article.ArticleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {
    private Long id;
    private String title;
    private String content;
    private String renderedContent;
    private String status;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ArticleResponse from(com.example.cms.domain.model.article.Article article, String categoryName) {
        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getContent(),
                article.getRenderedContent().getHtml(),
                article.getStatus().name(),
                article.getCategoryId(),
                categoryName,
                article.getPublishedAt(),
                article.getAudit().getCreatedAt(),
                article.getAudit().getUpdatedAt()
        );
    }
}
```

### Step 6: 验证编译

运行：`./gradlew compileJava`
预期输出：BUILD SUCCESSFUL

### Step 7: Commit

```bash
git add src/main/java/com/example/cms/presentation/dto/
git commit -m "feat: 创建展示层 DTO"
```

---

## Task 12: 展示层 - 后台 API Controller

**目标：** 实现后台管理 API。

**文件：**
- 创建：`src/main/java/com/example/cms/presentation/controller/admin/AdminCategoryController.java`
- 创建：`src/main/java/com/example/cms/presentation/controller/admin/AdminArticleController.java`

### Step 1: 创建 AdminCategoryController

创建 `src/main/java/com/example/cms/presentation/controller/admin/AdminCategoryController.java`：

```java
package com.example.cms.presentation.controller.admin;

import com.example.cms.application.service.CategoryService;
import com.example.cms.domain.model.category.Category;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.CategoryRequest;
import com.example.cms.presentation.dto.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAll() {
        List<CategoryResponse> responses = categoryService.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@RequestBody CategoryRequest request) {
        Category category = categoryService.create(request.getName(), request.getDescription());
        return ApiResponse.success(CategoryResponse.from(category));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @RequestBody CategoryRequest request) {
        Category category = categoryService.update(id, request.getName(), request.getDescription());
        return ApiResponse.success(CategoryResponse.from(category));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success(null);
    }
}
```

### Step 2: 创建 AdminArticleController

创建 `src/main/java/com/example/cms/presentation/controller/admin/AdminArticleController.java`：

```java
package com.example.cms.presentation.controller.admin;

import com.example.cms.application.service.ArticleService;
import com.example.cms.domain.model.article.Article;
import com.example.cms.domain.model.category.Category;
import com.example.cms.application.service.CategoryService;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.ArticleRequest;
import com.example.cms.presentation.dto.ArticleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<ArticleResponse>> getAll() {
        List<Article> articles = articleService.findAll();
        return ApiResponse.success(articles.stream()
                .map(a -> toResponse(a))
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleResponse> getById(@PathVariable Long id) {
        return articleService.findById(id)
                .map(article -> ApiResponse.success(toResponse(article)))
                .orElse(ApiResponse.error("文章不存在: " + id));
    }

    @PostMapping
    public ApiResponse<ArticleResponse> create(@RequestBody ArticleRequest request) {
        Article article = articleService.create(
                request.getTitle(),
                request.getContent(),
                request.getCategoryId()
        );
        return ApiResponse.success(toResponse(article));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArticleResponse> update(@PathVariable Long id, @RequestBody ArticleRequest request) {
        Article article = articleService.update(
                id,
                request.getTitle(),
                request.getContent(),
                request.getCategoryId()
        );
        return ApiResponse.success(toResponse(article));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        articleService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/publish")
    public ApiResponse<ArticleResponse> publish(@PathVariable Long id) {
        Article article = articleService.publish(id);
        return ApiResponse.success(toResponse(article));
    }

    @PostMapping("/{id}/unpublish")
    public ApiResponse<ArticleResponse> unpublish(@PathVariable Long id) {
        Article article = articleService.unpublish(id);
        return ApiResponse.success(toResponse(article));
    }

    private ArticleResponse toResponse(Article article) {
        String categoryName = categoryService.findById(article.getCategoryId())
                .map(Category::getName)
                .orElse("未知分类");
        return ArticleResponse.from(article, categoryName);
    }
}
```

### Step 3: 验证编译

运行：`./gradlew compileJava`
预期输出：BUILD SUCCESSFUL

### Step 4: Commit

```bash
git add src/main/java/com/example/cms/presentation/controller/admin/
git commit -m "feat: 实现后台管理 API"
```

---

## Task 13: 展示层 - 前台 API Controller

**目标：** 实现前台展示 API。

**文件：**
- 创建：`src/main/java/com/example/cms/presentation/controller/public/PublicCategoryController.java`
- 创建：`src/main/java/com/example/cms/presentation/controller/public/PublicArticleController.java`

### Step 1: 创建 PublicCategoryController

创建 `src/main/java/com/example/cms/presentation/controller/public/PublicCategoryController.java`：

```java
package com.example.cms.presentation.controller.public;

import com.example.cms.application.service.CategoryService;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAll() {
        List<CategoryResponse> responses = categoryService.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }
}
```

### Step 2: 创建 PublicArticleController

创建 `src/main/java/com/example/cms/presentation/controller/public/PublicArticleController.java`：

```java
package com.example.cms.presentation.controller.public;

import com.example.cms.application.service.ArticleService;
import com.example.cms.domain.model.article.Article;
import com.example.cms.domain.model.category.Category;
import com.example.cms.application.service.CategoryService;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.ArticleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/articles")
@RequiredArgsConstructor
public class PublicArticleController {

    private final ArticleService articleService;
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<ArticleResponse>> getPublished(
            @RequestParam(required = false) Long categoryId) {
        List<Article> articles = categoryId != null
                ? articleService.findByCategory(categoryId)
                : articleService.findPublished();

        return ApiResponse.success(articles.stream()
                .map(this::toResponse)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<ArticleResponse> getById(@PathVariable Long id) {
        return articleService.findById(id)
                .filter(article -> article.getStatus().name().equals("PUBLISHED"))
                .map(article -> ApiResponse.success(toResponse(article)))
                .orElse(ApiResponse.error(404, "文章不存在或未发布"));
    }

    private ArticleResponse toResponse(Article article) {
        String categoryName = categoryService.findById(article.getCategoryId())
                .map(Category::getName)
                .orElse("未知分类");
        return ArticleResponse.from(article, categoryName);
    }
}
```

### Step 3: 验证编译和启动

运行：`./gradlew build`
预期输出：BUILD SUCCESSFUL

运行：`./gradlew bootRun`
预期输出：应用启动成功，监听 8080 端口

### Step 4: Commit

```bash
git add src/main/java/com/example/cms/presentation/controller/public/
git commit -m "feat: 实现前台展示 API"
```

---

## Task 14: 前端项目初始化

**目标：** 创建 Vue 3 前端项目。

**文件：**
- 创建：`frontend/package.json`
- 创建：`frontend/vite.config.js`
- 创建：`frontend/index.html`
- 创建：`frontend/src/main.js`
- 创建：`frontend/src/App.vue`

### Step 1: 创建 package.json

创建 `frontend/package.json`：

```json
{
  "name": "cms-frontend",
  "version": "0.0.1",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "vue": "^3.4.15",
    "vue-router": "^4.2.5",
    "element-plus": "^2.4.4",
    "axios": "^1.6.5"
  },
  "devDependencies": {
    "@vitejs/plugin-vue": "^5.0.3",
    "vite": "^5.0.11"
  }
}
```

### Step 2: 创建 vite.config.js

创建 `frontend/vite.config.js`：

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  build: {
    outDir: 'dist',
    emptyOutDir: true
  }
})
```

### Step 3: 创建 index.html

创建 `frontend/index.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CMS 系统</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

### Step 4: 创建 main.js

创建 `frontend/src/main.js`：

```js
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.mount('#app')
```

### Step 5: 创建 App.vue

创建 `frontend/src/App.vue`：

```vue
<template>
  <router-view />
</template>

<script setup>
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}
</style>
```

### Step 6: 创建 router 配置

创建 `frontend/src/router/index.js`：

```js
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('../public/views/Home.vue')
    },
    {
      path: '/category/:id',
      component: () => import('../public/views/CategoryArticleList.vue')
    },
    {
      path: '/article/:id',
      component: () => import('../public/views/ArticleDetail.vue')
    },
    {
      path: '/admin',
      component: () => import('../admin/views/AdminLayout.vue'),
      children: [
        {
          path: '',
          redirect: '/admin/articles'
        },
        {
          path: 'categories',
          component: () => import('../admin/views/CategoryList.vue')
        },
        {
          path: 'categories/new',
          component: () => import('../admin/views/CategoryForm.vue')
        },
        {
          path: 'categories/:id/edit',
          component: () => import('../admin/views/CategoryForm.vue')
        },
        {
          path: 'articles',
          component: () => import('../admin/views/ArticleList.vue')
        },
        {
          path: 'articles/new',
          component: () => import('../admin/views/ArticleForm.vue')
        },
        {
          path: 'articles/:id/edit',
          component: () => import('../admin/views/ArticleForm.vue')
        }
      ]
    }
  ]
})

export default router
```

### Step 7: 创建 API 工具

创建 `frontend/src/api/index.js`：

```js
import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

api.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

export default api
```

### Step 8: 安装依赖

运行：`cd frontend && npm install`
预期输出：依赖安装成功

### Step 9: Commit

```bash
git add frontend/
git commit -m "feat: 初始化前端项目结构"
```

---

## Task 15: 前端 - API 服务

**目标：** 创建前后端交互的 API 服务。

**文件：**
- 创建：`frontend/src/api/category.js`
- 创建：`frontend/src/api/article.js`

### Step 1: 创建 category API 服务

创建 `frontend/src/api/category.js`：

```js
import api from './index'

export const categoryApi = {
  // 前台
  getAll: () => api.get('/public/categories'),

  // 后台
  adminGetAll: () => api.get('/admin/categories'),
  create: (data) => api.post('/admin/categories', data),
  update: (id, data) => api.put(`/admin/categories/${id}`, data),
  delete: (id) => api.delete(`/admin/categories/${id}`)
}
```

### Step 2: 创建 article API 服务

创建 `frontend/src/api/article.js`：

```js
import api from './index'

export const articleApi = {
  // 前台
  getPublished: (categoryId) => api.get('/public/articles', { params: { categoryId } }),
  getById: (id) => api.get(`/public/articles/${id}`),

  // 后台
  adminGetAll: () => api.get('/admin/articles'),
  adminGetById: (id) => api.get(`/admin/articles/${id}`),
  create: (data) => api.post('/admin/articles', data),
  update: (id, data) => api.put(`/admin/articles/${id}`, data),
  delete: (id) => api.delete(`/admin/articles/${id}`),
  publish: (id) => api.post(`/admin/articles/${id}/publish`),
  unpublish: (id) => api.post(`/admin/articles/${id}/unpublish`)
}
```

### Step 3: Commit

```bash
git add frontend/src/api/
git commit -m "feat: 创建前端 API 服务"
```

---

## Task 16: 前端 - 后台管理页面

**目标：** 实现后台管理页面。

**文件：**
- 创建：`frontend/src/admin/views/AdminLayout.vue`
- 创建：`frontend/src/admin/views/CategoryList.vue`
- 创建：`frontend/src/admin/views/CategoryForm.vue`
- 创建：`frontend/src/admin/views/ArticleList.vue`
- 创建：`frontend/src/admin/views/ArticleForm.vue`

### Step 1: 创建 AdminLayout

创建 `frontend/src/admin/views/AdminLayout.vue`：

```vue
<template>
  <el-container style="height: 100vh">
    <el-aside width="200px" style="background-color: #545c64">
      <div style="color: white; padding: 20px; font-size: 18px; font-weight: bold">
        CMS 后台
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#545c64"
        text-color="#fff"
        active-text-color="#ffd04b"
      >
        <el-menu-item index="/admin/articles">
          <el-icon><Document /></el-icon>
          <span>文章管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/categories">
          <el-icon><Folder /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/">
          <el-icon><Back /></el-icon>
          <span>返回前台</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-main>
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Document, Folder, Back } from '@element-plus/icons-vue'

const route = useRoute()
const activeMenu = computed(() => route.path)
</script>
```

### Step 2: 创建 CategoryList

创建 `frontend/src/admin/views/CategoryList.vue`：

```vue
<template>
  <div>
    <div style="margin-bottom: 20px; display: flex; justify-content: space-between">
      <h2>分类管理</h2>
      <el-button type="primary" @click="$router.push('/admin/categories/new')">
        新建分类
      </el-button>
    </div>

    <el-table :data="categories" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" width="200" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="edit(row.id)">编辑</el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'

const router = useRouter()
const categories = ref([])

const load = async () => {
  try {
    const res = await categoryApi.adminGetAll()
    categories.value = res.data
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const edit = (id) => {
  router.push(`/admin/categories/${id}/edit`)
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该分类吗？', '确认', { type: 'warning' })
    await categoryApi.delete(id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

onMounted(load)
</script>
```

### Step 3: 创建 CategoryForm

创建 `frontend/src/admin/views/CategoryForm.vue`：

```vue
<template>
  <div style="max-width: 600px">
    <h2>{{ isEdit ? '编辑分类' : '新建分类' }}</h2>

    <el-form :model="form" label-width="80px" style="margin-top: 20px">
      <el-form-item label="名称">
        <el-input v-model="form.name" placeholder="请输入分类名称" />
      </el-form-item>

      <el-form-item label="描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="请输入描述"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { categoryApi } from '@/api/category'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const id = computed(() => route.params.id)

const form = ref({
  name: '',
  description: ''
})

const load = async () => {
  if (!isEdit.value) return
  try {
    const res = await categoryApi.adminGetAll()
    const category = res.data.find(c => c.id === Number(id.value))
    if (category) {
      form.value = { name: category.name, description: category.description }
    }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const save = async () => {
  try {
    if (isEdit.value) {
      await categoryApi.update(id.value, form.value)
    } else {
      await categoryApi.create(form.value)
    }
    ElMessage.success('保存成功')
    router.push('/admin/categories')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

onMounted(load)
</script>
```

### Step 4: 创建 ArticleList

创建 `frontend/src/admin/views/ArticleList.vue`：

```vue
<template>
  <div>
    <div style="margin-bottom: 20px; display: flex; justify-content: space-between">
      <h2>文章管理</h2>
      <el-button type="primary" @click="$router.push('/admin/articles/new')">
        新建文章
      </el-button>
    </div>

    <el-table :data="articles" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" width="200" />
      <el-table-column prop="categoryName" label="分类" width="150" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'">
            {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString() }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300">
        <template #default="{ row }">
          <el-button size="small" @click="edit(row.id)">编辑</el-button>
          <el-button
            v-if="row.status === 'DRAFT'"
            size="small"
            type="success"
            @click="publish(row.id)"
          >
            发布
          </el-button>
          <el-button
            v-if="row.status === 'PUBLISHED'"
            size="small"
            type="warning"
            @click="unpublish(row.id)"
          >
            取消发布
          </el-button>
          <el-button size="small" type="danger" @click="remove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { articleApi } from '@/api/article'

const router = useRouter()
const articles = ref([])

const load = async () => {
  try {
    const res = await articleApi.adminGetAll()
    articles.value = res.data
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const edit = (id) => {
  router.push(`/admin/articles/${id}/edit`)
}

const publish = async (id) => {
  try {
    await articleApi.publish(id)
    ElMessage.success('发布成功')
    load()
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

const unpublish = async (id) => {
  try {
    await articleApi.unpublish(id)
    ElMessage.success('已取消发布')
    load()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const remove = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该文章吗？', '确认', { type: 'warning' })
    await articleApi.delete(id)
    ElMessage.success('删除成功')
    load()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(load)
</script>
```

### Step 5: 创建 ArticleForm

创建 `frontend/src/admin/views/ArticleForm.vue`：

```vue
<template>
  <div>
    <h2>{{ isEdit ? '编辑文章' : '新建文章' }}</h2>

    <el-form :model="form" label-width="80px" style="margin-top: 20px; max-width: 800px">
      <el-form-item label="标题">
        <el-input v-model="form.title" placeholder="请输入文章标题" />
      </el-form-item>

      <el-form-item label="分类">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
          <el-option
            v-for="cat in categories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="内容">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="20"
          placeholder="请输入 Markdown 内容"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="save">保存</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const id = computed(() => route.params.id)

const categories = ref([])

const form = ref({
  title: '',
  content: '',
  categoryId: null
})

const loadCategories = async () => {
  try {
    const res = await categoryApi.adminGetAll()
    categories.value = res.data
  } catch (e) {
    ElMessage.error('加载分类失败')
  }
}

const load = async () => {
  if (!isEdit.value) return
  try {
    const res = await articleApi.adminGetById(id.value)
    const article = res.data
    form.value = {
      title: article.title,
      content: article.content,
      categoryId: article.categoryId
    }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const save = async () => {
  if (!form.value.title || !form.value.content || !form.value.categoryId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    if (isEdit.value) {
      await articleApi.update(id.value, form.value)
    } else {
      await articleApi.create(form.value)
    }
    ElMessage.success('保存成功')
    router.push('/admin/articles')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}

onMounted(() => {
  loadCategories()
  load()
})
</script>
```

### Step 6: Commit

```bash
git add frontend/src/admin/
git commit -m "feat: 实现后台管理页面"
```

---

## Task 17: 前端 - 前台展示页面

**目标：** 实现前台展示页面。

**文件：**
- 创建：`frontend/src/public/views/Home.vue`
- 创建：`frontend/src/public/views/CategoryArticleList.vue`
- 创建：`frontend/src/public/views/ArticleDetail.vue`

### Step 1: 创建 Home 页面

创建 `frontend/src/public/views/Home.vue`：

```vue
<template>
  <div class="home">
    <header class="header">
      <div class="header-content">
        <h1 class="title">CMS 系统</h1>
        <nav class="nav">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link v-for="cat in categories" :key="cat.id" :to="`/category/${cat.id}`" class="nav-link">
            {{ cat.name }}
          </router-link>
          <router-link to="/admin" class="nav-link admin">后台管理</router-link>
        </nav>
      </div>
    </header>

    <main class="main">
      <div class="article-list">
        <div v-for="article in articles" :key="article.id" class="article-card" @click="viewArticle(article.id)">
          <h3 class="article-title">{{ article.title }}</h3>
          <div class="article-meta">
            <span class="category">{{ article.categoryName }}</span>
            <span class="time">{{ formatDate(article.publishedAt) }}</span>
          </div>
          <div class="article-preview">{{ article.renderedContent?.slice(0, 200) }}...</div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'

const router = useRouter()
const articles = ref([])
const categories = ref([])

const loadArticles = async () => {
  try {
    const res = await articleApi.getPublished()
    articles.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.getAll()
    categories.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const viewArticle = (id) => {
  router.push(`/article/${id}`)
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString()
}

onMounted(() => {
  loadArticles()
  loadCategories()
})
</script>

<style scoped>
.home {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.header-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  margin: 0;
  font-size: 24px;
}

.nav {
  display: flex;
  gap: 20px;
}

.nav-link {
  text-decoration: none;
  color: #333;
  padding: 8px 16px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.nav-link:hover,
.nav-link.router-link-active {
  background-color: #f0f0f0;
}

.nav-link.admin {
  color: #409eff;
}

.main {
  max-width: 800px;
  margin: 40px auto;
  padding: 0 20px;
}

.article-card {
  background-color: #fff;
  padding: 24px;
  margin-bottom: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: box-shadow 0.3s;
}

.article-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.article-title {
  margin: 0 0 12px;
  font-size: 20px;
}

.article-meta {
  display: flex;
  gap: 16px;
  color: #999;
  font-size: 14px;
  margin-bottom: 12px;
}

.article-preview {
  color: #666;
  line-height: 1.6;
}
</style>
```

### Step 2: 创建 CategoryArticleList 页面

创建 `frontend/src/public/views/CategoryArticleList.vue`：

```vue
<template>
  <div class="category-page">
    <header class="header">
      <div class="header-content">
        <h1 class="title">CMS 系统</h1>
        <nav class="nav">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link v-for="cat in categories" :key="cat.id" :to="`/category/${cat.id}`" class="nav-link">
            {{ cat.name }}
          </router-link>
          <router-link to="/admin" class="nav-link admin">后台管理</router-link>
        </nav>
      </div>
    </header>

    <main class="main">
      <h2 class="category-title">{{ category?.name || '加载中...' }}</h2>
      <div class="article-list">
        <div v-for="article in articles" :key="article.id" class="article-card" @click="viewArticle(article.id)">
          <h3 class="article-title">{{ article.title }}</h3>
          <div class="article-meta">
            <span class="time">{{ formatDate(article.publishedAt) }}</span>
          </div>
        </div>
        <div v-if="articles.length === 0" class="empty">该分类下暂无文章</div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'

const route = useRoute()
const router = useRouter()

const categoryId = computed(() => route.params.id)
const articles = ref([])
const categories = ref([])
const category = ref(null)

const loadArticles = async () => {
  try {
    const res = await articleApi.getPublished(categoryId.value)
    articles.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.getAll()
    categories.value = res.data
    category.value = res.data.find(c => c.id === Number(categoryId.value))
  } catch (e) {
    console.error(e)
  }
}

const viewArticle = (id) => {
  router.push(`/article/${id}`)
}

const formatDate = (date) => {
  return new Date(date).toLocaleDateString()
}

onMounted(() => {
  loadCategories()
  loadArticles()
})
</script>

<style scoped>
.category-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.header-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  margin: 0;
  font-size: 24px;
}

.nav {
  display: flex;
  gap: 20px;
}

.nav-link {
  text-decoration: none;
  color: #333;
  padding: 8px 16px;
  border-radius: 4px;
}

.nav-link:hover,
.nav-link.router-link-active {
  background-color: #f0f0f0;
}

.nav-link.admin {
  color: #409eff;
}

.main {
  max-width: 800px;
  margin: 40px auto;
  padding: 0 20px;
}

.category-title {
  margin-bottom: 24px;
}

.article-card {
  background-color: #fff;
  padding: 20px;
  margin-bottom: 12px;
  border-radius: 8px;
  cursor: pointer;
}

.article-card:hover {
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.article-title {
  margin: 0 0 8px;
  font-size: 18px;
}

.article-meta {
  color: #999;
  font-size: 14px;
}

.empty {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
```

### Step 3: 创建 ArticleDetail 页面

创建 `frontend/src/public/views/ArticleDetail.vue`：

```vue
<template>
  <div class="article-page">
    <header class="header">
      <div class="header-content">
        <h1 class="title">CMS 系统</h1>
        <nav class="nav">
          <router-link to="/" class="nav-link">首页</router-link>
          <router-link v-for="cat in categories" :key="cat.id" :to="`/category/${cat.id}`" class="nav-link">
            {{ cat.name }}
          </router-link>
          <router-link to="/admin" class="nav-link admin">后台管理</router-link>
        </nav>
      </div>
    </header>

    <main class="main">
      <div v-if="article" class="article">
        <h1 class="article-title">{{ article.title }}</h1>
        <div class="article-meta">
          <span class="category">{{ article.categoryName }}</span>
          <span class="time">{{ formatDate(article.publishedAt) }}</span>
        </div>
        <div class="article-content" v-html="article.renderedContent"></div>
      </div>
      <div v-else class="loading">加载中...</div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'

const route = useRoute()
const router = useRouter()

const articleId = computed(() => route.params.id)
const article = ref(null)
const categories = ref([])

const loadArticle = async () => {
  try {
    const res = await articleApi.getById(articleId.value)
    article.value = res.data
  } catch (e) {
    router.push('/')
  }
}

const loadCategories = async () => {
  try {
    const res = await categoryApi.getAll()
    categories.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const formatDate = (date) => {
  return new Date(date).toLocaleString()
}

onMounted(() => {
  loadCategories()
  loadArticle()
})
</script>

<style scoped>
.article-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.header-content {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  margin: 0;
  font-size: 24px;
}

.nav {
  display: flex;
  gap: 20px;
}

.nav-link {
  text-decoration: none;
  color: #333;
  padding: 8px 16px;
  border-radius: 4px;
}

.nav-link:hover,
.nav-link.router-link-active {
  background-color: #f0f0f0;
}

.nav-link.admin {
  color: #409eff;
}

.main {
  max-width: 800px;
  margin: 40px auto;
  padding: 0 20px;
}

.article {
  background-color: #fff;
  padding: 40px;
  border-radius: 8px;
}

.article-title {
  margin: 0 0 16px;
  font-size: 32px;
}

.article-meta {
  display: flex;
  gap: 16px;
  color: #999;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.article-content {
  line-height: 1.8;
  color: #333;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  margin-top: 24px;
  margin-bottom: 16px;
}

.article-content :deep(p) {
  margin-bottom: 16px;
}

.article-content :deep(code) {
  background-color: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
}

.article-content :deep(pre) {
  background-color: #f5f5f5;
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin-bottom: 16px;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #999;
}
</style>
```

### Step 4: Commit

```bash
git add frontend/src/public/
git commit -m "feat: 实现前台展示页面"
```

---

## Task 18: 前后端集成配置

**目标：** 配置前端构建产物集成到后端。

**文件：**
- 修改：`build.gradle`
- 创建：`frontend/src/api/index.js`（更新代理配置）

### Step 1: 更新 build.gradle 添加前端构建任务

修改 `build.gradle`，添加前端构建集成：

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.1'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Flyway
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'

    // H2 Database
    runtimeOnly 'com.h2database:h2'

    // Markdown Renderer
    implementation 'com.vladsch.flexmark:flexmark-all:0.64.8'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

// 前端构建任务
task buildFrontend(type: Exec) {
    workingDir file('frontend')
    inputs.dir file('frontend/src')
    outputs.dir file('frontend/dist')

    if (System.getProperty('os.name').toLowerCase().contains('windows')) {
        commandLine 'npm', 'run', 'build'
    } else {
        commandLine 'sh', '-c', 'npm install && npm run build'
    }
}

// 复制前端构建产物到 static 目录
task copyFrontend(type: Copy) {
    dependsOn buildFrontend
    from file('frontend/dist')
    into file('src/main/resources/static')
}

// 确保 bootRun 前先构建前端
bootRun.dependsOn copyFrontend
// 确保 bootJar 前先构建前端
bootJar.dependsOn copyFrontend

tasks.named('test') {
    useJUnitPlatform()
}
```

### Step 2: 更新 vite.config.js 添加基础路径

修改 `frontend/vite.config.js`：

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  base: '/',
  build: {
    outDir: 'dist',
    emptyOutDir: true
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### Step 3: 验证完整构建

运行：`./gradlew build`
预期输出：BUILD SUCCESSFUL

### Step 4: 启动应用验证

运行：`./gradlew bootRun`
预期输出：应用启动成功

访问验证：
- http://localhost:8080/ - 前台首页
- http://localhost:8080/admin - 后台管理
- http://localhost:8080/api/public/categories - API 测试

### Step 5: Commit

```bash
git add build.gradle frontend/vite.config.js
git commit -m "feat: 配置前后端集成构建"
```

---

## 验收标准

### 后端 API

- [ ] `GET /api/admin/categories` - 返回所有分类
- [ ] `POST /api/admin/categories` - 创建分类
- [ ] `PUT /api/admin/categories/{id}` - 更新分类
- [ ] `DELETE /api/admin/categories/{id}` - 删除分类
- [ ] `GET /api/admin/articles` - 返回所有文章
- [ ] `POST /api/admin/articles` - 创建文章（自动渲染 MD→HTML）
- [ ] `PUT /api/admin/articles/{id}` - 更新文章
- [ ] `DELETE /api/admin/articles/{id}` - 删除文章
- [ ] `POST /api/admin/articles/{id}/publish` - 发布文章
- [ ] `GET /api/public/articles` - 返回已发布文章
- [ ] `GET /api/public/articles/{id}` - 返回文章详情

### 前端功能

- [ ] 前台首页展示文章列表
- [ ] 前台分类页面展示该分类下的文章
- [ ] 前台文章详情页正确渲染 HTML
- [ ] 后台分类管理（增删改查）
- [ ] 后台文章管理（增删改查）
- [ ] 后台文章发布/取消发布
- [ ] Markdown 内容正确渲染为 HTML

### 技术要求

- [ ] DDD 分层架构清晰
- [ ] 领域模型使用充血模型
- [ ] Repository 接口在领域层定义
- [ ] 前后端一体化部署成功

---

**计划完成！** 🎉
