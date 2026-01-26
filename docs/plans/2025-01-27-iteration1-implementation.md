# 迭代1：多级分类与富文本增强 - 实现计划

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**目标：** 实现多级分类树结构、通用排序表、增强 Markdown 编辑器、前台导航栏

**架构：** 通过独立的 `sort_orders` 表统一管理分类和文章的层级+排序，前端集成 md-editor-v3 实现富文本编辑和图片 Base64 嵌入

**技术栈：** Spring Boot 3.2.1 + JPA + Flyway + Vue 3 + Element Plus + md-editor-v3

---

## 阶段一：数据库层 - 通用排序表

### Task 1: 创建 Flyway 迁移 - sort_orders 表

**文件：**
- 创建: `src/main/resources/db/migration/V3__Create_sort_orders_table.sql`

**步骤 1: 创建迁移文件**

创建 `V3__Create_sort_orders_table.sql`：

```sql
-- 通用排序表（唯一的层级+排序存储）
CREATE TABLE sort_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_type VARCHAR(20) NOT NULL COMMENT '资源类型: CATEGORY, ARTICLE',
    resource_id BIGINT NOT NULL COMMENT '资源ID',
    parent_type VARCHAR(20) COMMENT '父资源类型: CATEGORY 或 NULL',
    parent_id BIGINT COMMENT '父资源ID，NULL表示根级',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '同级排序序号，从0开始',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_resource (resource_type, resource_id),
    INDEX idx_parent (parent_type, parent_id)
) COMMENT='通用排序表，管理资源的层级结构和排序';
```

**步骤 2: 启动应用验证迁移**

运行: `./gradlew bootRun`

预期: 应用成功启动，`sort_orders` 表被创建

**步骤 3: 验证表结构**

在 H2 控制台或 MySQL 客户端执行:

```sql
DESCRIBE sort_orders;
-- 应该显示: id, resource_type, resource_id, parent_type, parent_id, sort_order, created_at, updated_at
```

**步骤 4: Commit**

```bash
git add src/main/resources/db/migration/V3__Create_sort_orders_table.sql
git commit -m "feat: 添加通用排序表 sort_orders

- 支持 CATEGORY 和 ARTICLE 两种资源类型
- 通过 parent_type + parent_id 定义层级关系
- sort_order 实现同级排序
"
```

---

### Task 2: 创建 SortOrderEntity

**文件：**
- 创建: `src/main/java/com/example/cms/infrastructure/persistence/entity/SortOrderEntity.java`

**步骤 1: 创建实体类**

创建 `SortOrderEntity.java`：

```java
package com.example.cms.infrastructure.persistence.entity;

import com.example.cms.domain.model.sortorder.SortOrder;
import com.example.cms.domain.model.sortorder.SortOrder.ResourceType;
import com.example.cms.infrastructure.persistence.entity.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sort_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SortOrderEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 20)
    private ResourceType resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "parent_type", length = 20)
    private ResourceType parentType;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public SortOrder toDomainModel() {
        return SortOrder.builder()
                .id(id)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .parentType(parentType)
                .parentId(parentId)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .audit(getAudit())
                .build();
    }

    public static SortOrderEntity fromDomainModel(SortOrder sortOrder) {
        return SortOrderEntity.builder()
                .id(sortOrder.getId())
                .resourceType(sortOrder.getResourceType())
                .resourceId(sortOrder.getResourceId())
                .parentType(sortOrder.getParentType())
                .parentId(sortOrder.getParentId())
                .sortOrder(sortOrder.getSortOrder())
                .audit(sortOrder.getAudit())
                .build();
    }
}
```

**步骤 2: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 3: Commit**

```bash
git add src/main/java/com/example/cms/infrastructure/persistence/entity/SortOrderEntity.java
git commit -m "feat: 添加 SortOrderEntity 实体

- 映射 sort_orders 表
- 支持 ResourceType 枚举
- 提供领域模型转换方法
"
```

---

### Task 3: 创建 SortOrder 领域模型

**文件：**
- 创建: `src/main/java/com/example/cms/domain/model/sortorder/SortOrder.java`
- 创建: `src/main/java/com/example/cms/domain/model/sortorder/ResourceType.java`

**步骤 1: 创建 ResourceType 枚举**

创建 `ResourceType.java`：

```java
package com.example.cms.domain.model.sortorder;

public enum ResourceType {
    CATEGORY("CATEGORY"),
    ARTICLE("ARTICLE");

    private final String value;

    ResourceType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ResourceType fromValue(String value) {
        for (ResourceType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown resource type: " + value);
    }
}
```

**步骤 2: 创建 SortOrder 领域模型**

创建 `SortOrder.java`：

```java
package com.example.cms.domain.model.sortorder;

import com.example.cms.domain.shared.Audit;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SortOrder {
    private final Long id;
    private final ResourceType resourceType;
    private final Long resourceId;
    private final ResourceType parentType;
    private final Long parentId;
    private int sortOrder;
    private final Audit audit;

    public void updateOrder(int newSortOrder) {
        this.sortOrder = newSortOrder;
        if (audit != null) {
            // 注意：这里需要 Audit 类支持 markModified，如果没有则暂时跳过
        }
    }

    public boolean isRootLevel() {
        return parentType == null || parentId == null;
    }
}
```

**步骤 3: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 4: Commit**

```bash
git add src/main/java/com/example/cms/domain/model/sortorder/
git commit -m "feat: 添加 SortOrder 领域模型

- ResourceType 枚举定义 CATEGORY 和 ARTICLE
- SortOrder 聚合根支持排序更新
- 判断是否为根级的方法
"
```

---

### Task 4: 创建 SortOrderRepository 接口

**文件：**
- 创建: `src/main/java/com/example/cms/domain/repository/SortOrderRepository.java`

**步骤 1: 创建仓储接口**

创建 `SortOrderRepository.java`：

```java
package com.example.cms.domain.repository;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;

import java.util.List;
import java.util.Optional;

public interface SortOrderRepository {
    SortOrder save(SortOrder sortOrder);
    Optional<SortOrder> findByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);
    List<SortOrder> findByParentTypeAndParentIdOrderBySortOrder(ResourceType parentType, Long parentId);
    List<SortOrder> findByResourceTypeOrderBySortOrder(ResourceType resourceType);
    List<SortOrder> findAll();
    void deleteByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);
    void deleteByParentTypeAndParentId(ResourceType parentType, Long parentId);
    void updateSortOrder(Long id, int newSortOrder);
    boolean existsByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);
}
```

**步骤 2: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 3: Commit**

```bash
git add src/main/java/com/example/cms/domain/repository/SortOrderRepository.java
git commit -m "feat: 添加 SortOrderRepository 接口

- 按资源类型和ID查找
- 按父资源查询子资源列表（排序）
- 批量删除子资源
- 更新排序序号
"
```

---

### Task 5: 实现 SortOrderRepository

**文件：**
- 创建: `src/main/java/com/example/cms/infrastructure/persistence/repository/SpringDataSortOrderRepository.java`
- 创建: `src/main/java/com/example/cms/infrastructure/persistence/repository/SortOrderRepositoryImpl.java`

**步骤 1: 创建 Spring Data 接口**

创建 `SpringDataSortOrderRepository.java`：

```java
package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.infrastructure.persistence.entity.SortOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataSortOrderRepository extends JpaRepository<SortOrderEntity, Long> {

    Optional<SortOrderEntity> findByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);

    List<SortOrderEntity> findByParentTypeAndParentIdOrderBySortOrderAsc(ResourceType parentType, Long parentId);

    List<SortOrderEntity> findByResourceTypeOrderBySortOrderAsc(ResourceType resourceType);

    boolean existsByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId);

    @Modifying
    @Query("UPDATE SortOrderEntity s SET s.sortOrder = :order WHERE s.id = :id")
    void updateSortOrder(@Param("id") Long id, @Param("order") int order);

    @Modifying
    @Query("DELETE FROM SortOrderEntity s WHERE s.resourceType = :resourceType AND s.resourceId = :resourceId")
    void deleteByResourceTypeAndResourceId(@Param("resourceType") ResourceType resourceType,
                                           @Param("resourceId") Long resourceId);

    @Modifying
    @Query("DELETE FROM SortOrderEntity s WHERE s.parentType = :parentType AND s.parentId = :parentId")
    void deleteByParentTypeAndParentId(@Param("parentType") ResourceType parentType,
                                       @Param("parentId") Long parentId);
}
```

**步骤 2: 创建实现类**

创建 `SortOrderRepositoryImpl.java`：

```java
package com.example.cms.infrastructure.persistence.repository;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;
import com.example.cms.domain.repository.SortOrderRepository;
import com.example.cms.infrastructure.persistence.entity.SortOrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SortOrderRepositoryImpl implements SortOrderRepository {

    private final SpringDataSortOrderRepository springDataSortOrderRepository;

    @Override
    public SortOrder save(SortOrder sortOrder) {
        SortOrderEntity entity = SortOrderEntity.fromDomainModel(sortOrder);
        SortOrderEntity saved = springDataSortOrderRepository.save(entity);
        return saved.toDomainModel();
    }

    @Override
    public Optional<SortOrder> findByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId) {
        return springDataSortOrderRepository
                .findByResourceTypeAndResourceId(resourceType, resourceId)
                .map(SortOrderEntity::toDomainModel);
    }

    @Override
    public List<SortOrder> findByParentTypeAndParentIdOrderBySortOrder(ResourceType parentType, Long parentId) {
        return springDataSortOrderRepository
                .findByParentTypeAndParentIdOrderBySortOrderAsc(parentType, parentId)
                .stream()
                .map(SortOrderEntity::toDomainModel)
                .toList();
    }

    @Override
    public List<SortOrder> findByResourceTypeOrderBySortOrder(ResourceType resourceType) {
        return springDataSortOrderRepository
                .findByResourceTypeOrderBySortOrderAsc(resourceType)
                .stream()
                .map(SortOrderEntity::toDomainModel)
                .toList();
    }

    @Override
    public List<SortOrder> findAll() {
        return springDataSortOrderRepository.findAll()
                .stream()
                .map(SortOrderEntity::toDomainModel)
                .toList();
    }

    @Override
    public void deleteByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId) {
        springDataSortOrderRepository.deleteByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Override
    public void deleteByParentTypeAndParentId(ResourceType parentType, Long parentId) {
        springDataSortOrderRepository.deleteByParentTypeAndParentId(parentType, parentId);
    }

    @Override
    public void updateSortOrder(Long id, int newSortOrder) {
        springDataSortOrderRepository.updateSortOrder(id, newSortOrder);
    }

    @Override
    public boolean existsByResourceTypeAndResourceId(ResourceType resourceType, Long resourceId) {
        return springDataSortOrderRepository.existsByResourceTypeAndResourceId(resourceType, resourceId);
    }
}
```

**步骤 3: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 4: Commit**

```bash
git add src/main/java/com/example/cms/infrastructure/persistence/repository/SpringDataSortOrderRepository.java
git add src/main/java/com/example/cms/infrastructure/persistence/repository/SortOrderRepositoryImpl.java
git commit -m "feat: 实现 SortOrderRepository

- Spring Data JPA 接口定义
- SortOrderRepositoryImpl 实现类
- 支持按父子关系查询、排序更新
"
```

---

## 阶段二：应用层 - 排序服务

### Task 6: 创建排序 DTO

**文件：**
- 创建: `src/main/java/com/example/cms/presentation/dto/ReorderRequest.java`
- 创建: `src/main/java/com/example/cms/presentation/dto/OrderItem.java`
- 创建: `src/main/java/com/example/cms/presentation/dto/MoveCategoryRequest.java`

**步骤 1: 创建 OrderItem DTO**

创建 `OrderItem.java`：

```java
package com.example.cms.presentation.dto;

import com.example.cms.domain.model.sortorder.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private ResourceType resourceType;
    private Long resourceId;
    private Integer sortOrder;
}
```

**步骤 2: 创建 ReorderRequest DTO**

创建 `ReorderRequest.java`：

```java
package com.example.cms.presentation.dto;

import com.example.cms.domain.model.sortorder.ResourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRequest {
    private ResourceType parentType;
    private Long parentId;  // null 表示根级
    @Valid
    @NotEmpty(message = "排序项不能为空")
    private List<OrderItem> items;
}
```

**步骤 3: 创建 MoveCategoryRequest DTO**

创建 `MoveCategoryRequest.java`：

```java
package com.example.cms.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveCategoryRequest {
    @NotNull(message = "目标父分类ID不能为空")
    private Long newParentId;  // null 表示移动到根级
}
```

**步骤 4: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 5: Commit**

```bash
git add src/main/java/com/example/cms/presentation/dto/ReorderRequest.java
git add src/main/java/com/example/cms/presentation/dto/OrderItem.java
git add src/main/java/com/example/cms/presentation/dto/MoveCategoryRequest.java
git commit -m "feat: 添加排序相关 DTO

- OrderItem: 单个排序项
- ReorderRequest: 批量排序请求
- MoveCategoryRequest: 移动分类请求
"
```

---

### Task 7: 创建 SortOrderService

**文件：**
- 创建: `src/main/java/com/example/cms/application/service/SortOrderService.java`

**步骤 1: 创建服务接口**

创建 `SortOrderService.java`：

```java
package com.example.cms.application.service;

import com.example.cms.presentation.dto.MoveCategoryRequest;
import com.example.cms.presentation.dto.ReorderRequest;
import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;

import java.util.List;

public interface SortOrderService {
    void reorder(ReorderRequest request);
    void initializeCategorySortOrder(Long categoryId, Long parentId, int sortOrder);
    void initializeArticleSortOrder(Long articleId, Long categoryId, int sortOrder);
    void moveCategoryToParent(Long categoryId, Long newParentId);
    SortOrder getCategorySortOrder(Long categoryId);
    SortOrder getArticleSortOrder(Long articleId);
    List<SortOrder> getChildrenByParent(ResourceType parentType, Long parentId);
    void deleteSortOrder(ResourceType resourceType, Long resourceId);
    void deleteChildrenSortOrders(ResourceType parentType, Long parentId);
}
```

**步骤 2: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 3: Commit**

```bash
git add src/main/java/com/example/cms/application/service/SortOrderService.java
git commit -m "feat: 添加 SortOrderService 接口

- reorder: 批量更新排序
- initialize*: 初始化分类/文章排序
- moveCategoryToParent: 移动分类到新父分类
- getChildrenByParent: 获取子资源列表
- delete*: 删除排序记录
"
```

---

### Task 8: 实现 SortOrderService

**文件：**
- 创建: `src/main/java/com/example/cms/application/service/SortOrderServiceImpl.java`

**步骤 1: 创建实现类**

创建 `SortOrderServiceImpl.java`：

```java
package com.example.cms.application.service;

import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;
import com.example.cms.domain.repository.SortOrderRepository;
import com.example.cms.presentation.dto.MoveCategoryRequest;
import com.example.cms.presentation.dto.OrderItem;
import com.example.cms.presentation.dto.ReorderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SortOrderServiceImpl implements SortOrderService {

    private final SortOrderRepository sortOrderRepository;

    @Override
    @Transactional
    public void reorder(ReorderRequest request) {
        for (OrderItem item : request.getItems()) {
            SortOrder sortOrder = sortOrderRepository
                    .findByResourceTypeAndResourceId(item.getResourceType(), item.getResourceId())
                    .orElseThrow(() -> new IllegalArgumentException(
                        "SortOrder not found for " + item.getResourceType() + ":" + item.getResourceId()));

            sortOrder.updateOrder(item.getSortOrder());
            sortOrderRepository.updateSortOrder(sortOrder.getId(), item.getSortOrder());
        }
    }

    @Override
    @Transactional
    public void initializeCategorySortOrder(Long categoryId, Long parentId, int sortOrder) {
        if (sortOrderRepository.existsByResourceTypeAndResourceId(ResourceType.CATEGORY, categoryId)) {
            return; // 已存在则跳过
        }

        SortOrder newSortOrder = SortOrder.builder()
                .resourceType(ResourceType.CATEGORY)
                .resourceId(categoryId)
                .parentType(parentId != null ? ResourceType.CATEGORY : null)
                .parentId(parentId)
                .sortOrder(sortOrder)
                .build();

        sortOrderRepository.save(newSortOrder);
    }

    @Override
    @Transactional
    public void initializeArticleSortOrder(Long articleId, Long categoryId, int sortOrder) {
        if (sortOrderRepository.existsByResourceTypeAndResourceId(ResourceType.ARTICLE, articleId)) {
            return;
        }

        SortOrder newSortOrder = SortOrder.builder()
                .resourceType(ResourceType.ARTICLE)
                .resourceId(articleId)
                .parentType(ResourceType.CATEGORY)
                .parentId(categoryId)
                .sortOrder(sortOrder)
                .build();

        sortOrderRepository.save(newSortOrder);
    }

    @Override
    @Transactional
    public void moveCategoryToParent(Long categoryId, Long newParentId) {
        SortOrder sortOrder = sortOrderRepository
                .findByResourceTypeAndResourceId(ResourceType.CATEGORY, categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类排序记录不存在: " + categoryId));

        // 计算新的排序序号（追加到末尾）
        int newSortOrder = 0;
        if (newParentId != null) {
            List<SortOrder> siblings = sortOrderRepository
                    .findByParentTypeAndParentIdOrderBySortOrder(ResourceType.CATEGORY, newParentId);
            newSortOrder = siblings.size();
        } else {
            List<SortOrder> rootCategories = sortOrderRepository
                    .findByParentTypeAndParentIdOrderBySortOrder(null, null);
            newSortOrder = rootCategories.size();
        }

        SortOrder updated = SortOrder.builder()
                .id(sortOrder.getId())
                .resourceType(ResourceType.CATEGORY)
                .resourceId(categoryId)
                .parentType(newParentId != null ? ResourceType.CATEGORY : null)
                .parentId(newParentId)
                .sortOrder(newSortOrder)
                .audit(sortOrder.getAudit())
                .build();

        sortOrderRepository.save(updated);
    }

    @Override
    public SortOrder getCategorySortOrder(Long categoryId) {
        return sortOrderRepository
                .findByResourceTypeAndResourceId(ResourceType.CATEGORY, categoryId)
                .orElse(null);
    }

    @Override
    public SortOrder getArticleSortOrder(Long articleId) {
        return sortOrderRepository
                .findByResourceTypeAndResourceId(ResourceType.ARTICLE, articleId)
                .orElse(null);
    }

    @Override
    public List<SortOrder> getChildrenByParent(ResourceType parentType, Long parentId) {
        return sortOrderRepository.findByParentTypeAndParentIdOrderBySortOrder(parentType, parentId);
    }

    @Override
    @Transactional
    public void deleteSortOrder(ResourceType resourceType, Long resourceId) {
        sortOrderRepository.deleteByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Override
    @Transactional
    public void deleteChildrenSortOrders(ResourceType parentType, Long parentId) {
        sortOrderRepository.deleteByParentTypeAndParentId(parentType, parentId);
    }
}
```

**步骤 2: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 3: Commit**

```bash
git add src/main/java/com/example/cms/application/service/SortOrderServiceImpl.java
git commit -m "feat: 实现 SortOrderService

- reorder: 批量更新排序
- initializeCategorySortOrder: 初始化分类排序（创建时调用）
- initializeArticleSortOrder: 初始化文章排序
- moveCategoryToParent: 移动分类到新父分类
- getChildrenByParent: 获取子资源（用于构建树）
- delete*: 删除排序记录
"
```

---

### Task 9: 创建分类树 DTO

**文件：**
- 创建: `src/main/java/com/example/cms/presentation/dto/CategoryTreeNode.java`
- 创建: `src/main/java/com/example/cms/presentation/dto/CategoryTreeResponse.java`

**步骤 1: 创建树节点 DTO**

创建 `CategoryTreeNode.java`：

```java
package com.example.cms.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeNode {
    private Long id;
    private String name;
    private String description;
    private Integer sortOrder;
    private List<CategoryTreeNode> children;

    public static CategoryTreeNode from(Long id, String name, String description, Integer sortOrder) {
        return CategoryTreeNode.builder()
                .id(id)
                .name(name)
                .description(description)
                .sortOrder(sortOrder)
                .children(new ArrayList<>())
                .build();
    }
}
```

**步骤 2: 创建树响应 DTO**

创建 `CategoryTreeResponse.java`：

```java
package com.example.cms.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryTreeResponse {
    private List<CategoryTreeNode> tree;
}
```

**步骤 3: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 4: Commit**

```bash
git add src/main/java/com/example/cms/presentation/dto/CategoryTreeNode.java
git add src/main/java/com/example/cms/presentation/dto/CategoryTreeResponse.java
git commit -m "feat: 添加分类树 DTO

- CategoryTreeNode: 树节点（含子节点列表）
- CategoryTreeResponse: 树响应
"
```

---

### Task 10: 扩展 CategoryRepository 支持排序查询

**文件：**
- 修改: `src/main/java/com/example/cms/domain/repository/CategoryRepository.java`

**步骤 1: 添加新方法**

在 `CategoryRepository.java` 中添加方法：

```java
package com.example.cms.domain.repository;

import com.example.cms.domain.model.category.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(Long id);
    List<Category> findAll();
    List<Category> findByIds(List<Long> ids);
    void deleteById(Long id);
    boolean existsByName(String name);
    boolean existsByCategoryId(Long id);  // 新增
}
```

**步骤 2: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL（会出现编译错误，因为实现类需要同步更新）

**步骤 3: 同步更新实现类**

修改 `src/main/java/com/example/cms/infrastructure/persistence/repository/CategoryRepositoryImpl.java`，添加：

```java
@Override
public List<Category> findByIds(List<Long> ids) {
    return springDataCategoryRepository.findAllById(ids)
            .stream()
            .map(CategoryEntity::toDomainModel)
            .toList();
}

@Override
public boolean existsByCategoryId(Long id) {
    return springDataCategoryRepository.existsById(id);
}
```

**步骤 4: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 5: Commit**

```bash
git add src/main/java/com/example/cms/domain/repository/CategoryRepository.java
git add src/main/java/com/example/cms/infrastructure/persistence/repository/CategoryRepositoryImpl.java
git commit -m "feat: CategoryRepository 新增批量查询

- findByIds: 批量查询分类
- existsByCategoryId: 判断分类是否存在
"
```

---

### Task 11: 扩展 CategoryService 支持树形操作

**文件：**
- 修改: `src/main/java/com/example/cms/application/service/CategoryService.java`

**步骤 1: 添加新方法**

在 `CategoryService.java` 中添加方法：

```java
package com.example.cms.application.service;

import com.example.cms.domain.model.category.Category;
import com.example.cms.domain.repository.ArticleRepository;
import com.example.cms.domain.repository.CategoryRepository;
import com.example.cms.domain.model.sortorder.ResourceType;
import com.example.cms.domain.model.sortorder.SortOrder;
import com.example.cms.presentation.dto.CategoryTreeResponse;
import com.example.cms.presentation.dto.CategoryTreeNode;
import com.example.cms.application.service.SortOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ArticleRepository articleRepository;
    private final SortOrderService sortOrderService;

    // ... 保留现有方法 ...

    @Transactional
    public CategoryTreeResponse getTree() {
        // 获取所有根级分类（parentType=null 或 parentId=null）
        List<SortOrder> rootSortOrders = sortOrderService.getChildrenByParent(null, null);

        if (rootSortOrders.isEmpty()) {
            return CategoryTreeResponse.builder().tree(new ArrayList<>()).build();
        }

        List<Long> rootIds = rootSortOrders.stream()
                .map(SortOrder::getResourceId)
                .toList();

        Map<Long, Category> categoryMap = categoryRepository.findByIds(rootIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<CategoryTreeNode> tree = new ArrayList<>();
        for (SortOrder so : rootSortOrders) {
            Category category = categoryMap.get(so.getResourceId());
            if (category != null) {
                tree.add(buildTreeNode(category, so.getSortOrder()));
            }
        }

        return CategoryTreeResponse.builder().tree(tree).build();
    }

    private CategoryTreeNode buildTreeNode(Category category, Integer sortOrder) {
        CategoryTreeNode node = CategoryTreeNode.from(
                category.getId(),
                category.getName(),
                category.getDescription(),
                sortOrder
        );

        // 递归获取子分类
        List<SortOrder> children = sortOrderService.getChildrenByParent(
                ResourceType.CATEGORY, category.getId()
        );

        if (!children.isEmpty()) {
            List<Long> childIds = children.stream()
                    .map(SortOrder::getResourceId)
                    .toList();

            Map<Long, Category> childMap = categoryRepository.findByIds(childIds).stream()
                    .collect(Collectors.toMap(Category::getId, c -> c));

            for (SortOrder child : children) {
                Category childCategory = childMap.get(child.getResourceId());
                if (childCategory != null) {
                    node.getChildren().add(buildTreeNode(childCategory, child.getSortOrder()));
                }
            }
        }

        return node;
    }

    @Transactional
    public void moveToCategory(Long categoryId, Long newParentId) {
        // 验证分类存在
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + categoryId));

        // 验证新父分类存在
        if (newParentId != null) {
            categoryRepository.findById(newParentId)
                    .orElseThrow(() -> new IllegalArgumentException("目标父分类不存在: " + newParentId));

            // 防止循环引用
            validateNoCycle(categoryId, newParentId);
        }

        sortOrderService.moveCategoryToParent(categoryId, newParentId);
    }

    private void validateNoCycle(Long categoryId, Long targetParentId) {
        Set<Long> visited = new HashSet<>();
        Long currentId = targetParentId;

        while (currentId != null) {
            if (visited.contains(currentId)) {
                throw new IllegalArgumentException("检测到循环引用");
            }
            if (currentId.equals(categoryId)) {
                throw new IllegalArgumentException("不能将分类移动到其子分类下");
            }
            visited.add(currentId);

            SortOrder currentSort = sortOrderService.getCategorySortOrder(currentId);
            if (currentSort == null || currentSort.getParentId() == null) {
                break;
            }
            currentId = currentSort.getParentId();
        }
    }

    @Transactional
    public void deleteWithCascade(Long categoryId, boolean cascade) {
        if (cascade) {
            // 递归删除所有子分类
            List<SortOrder> children = sortOrderService.getChildrenByParent(
                    ResourceType.CATEGORY, categoryId
            );

            for (SortOrder child : children) {
                deleteWithCascade(child.getResourceId(), true);
            }
        } else {
            // 检查是否有子分类
            List<SortOrder> children = sortOrderService.getChildrenByParent(
                    ResourceType.CATEGORY, categoryId
            );
            if (!children.isEmpty()) {
                throw new IllegalArgumentException("该分类下有子分类，无法删除");
            }
        }

        // 检查是否有关联文章
        if (articleRepository.existsByCategoryId(categoryId)) {
            throw new IllegalArgumentException("该分类下有文章，无法删除");
        }

        sortOrderService.deleteSortOrder(ResourceType.CATEGORY, categoryId);
        categoryRepository.deleteById(categoryId);
    }

    @Transactional
    public Category create(String name, String description, Long parentId, int sortOrder) {
        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("分类名称已存在: " + name);
        }

        // 验证父分类存在
        if (parentId != null) {
            categoryRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("父分类不存在: " + parentId));
        }

        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();

        Category saved = categoryRepository.save(category);

        // 初始化排序
        sortOrderService.initializeCategorySortOrder(saved.getId(), parentId, sortOrder);

        return saved;
    }
}
```

**步骤 2: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 3: Commit**

```bash
git add src/main/java/com/example/cms/application/service/CategoryService.java
git commit -m "feat: CategoryService 支持树形操作

- getTree: 获取完整分类树（递归构建）
- moveToCategory: 移动分类到新父分类
- validateNoCycle: 防止循环引用
- deleteWithCascade: 级联删除分类
- create: 重载支持父分类和排序
"
```

---

## 阶段三：控制器层 - API 端点

### Task 12: 扩展 AdminCategoryController

**文件：**
- 修改: `src/main/java/com/example/cms/presentation/controller/admin/AdminCategoryController.java`

**步骤 1: 添加新端点**

修改 `AdminCategoryController.java`：

```java
package com.example.cms.presentation.controller.admin;

import com.example.cms.application.service.CategoryService;
import com.example.cms.application.service.SortOrderService;
import com.example.cms.domain.model.category.Category;
import com.example.cms.presentation.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final SortOrderService sortOrderService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAll() {
        List<CategoryResponse> responses = categoryService.findAll().stream()
                .map(CategoryResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/tree")
    public ApiResponse<CategoryTreeResponse> getTree() {
        return ApiResponse.success(categoryService.getTree());
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@RequestBody @Valid CategoryRequest request) {
        Category category = categoryService.create(
                request.getName(),
                request.getDescription(),
                request.getParentId(),
                request.getSortOrder() != null ? request.getSortOrder() : 0
        );
        return ApiResponse.success(CategoryResponse.from(category));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @RequestBody @Valid CategoryRequest request) {
        Category category = categoryService.update(id, request.getName(), request.getDescription());
        return ApiResponse.success(CategoryResponse.from(category));
    }

    @PutMapping("/{id}/move")
    public ApiResponse<Void> moveToCategory(@PathVariable Long id, @RequestBody @Valid MoveCategoryRequest request) {
        categoryService.moveToCategory(id, request.getNewParentId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean cascade) {
        categoryService.deleteWithCascade(id, cascade);
        return ApiResponse.success(null);
    }
}
```

**步骤 2: 扩展 CategoryRequest DTO**

修改 `src/main/java/com/example/cms/presentation/dto/CategoryRequest.java`：

```java
package com.example.cms.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {
    @NotBlank(message = "分类名称不能为空")
    private String name;

    private String description;

    private Long parentId;       // 新增：父分类ID

    private Integer sortOrder;   // 新增：排序序号
}
```

**步骤 3: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 4: 启动验证**

运行: `./gradlew bootRun`

预期: 应用成功启动

**步骤 5: Commit**

```bash
git add src/main/java/com/example/cms/presentation/controller/admin/AdminCategoryController.java
git add src/main/java/com/example/cms/presentation/dto/CategoryRequest.java
git commit -m "feat: AdminCategoryController 新增树形操作端点

- GET /api/admin/categories/tree: 获取分类树
- PUT /api/admin/categories/{id}/move: 移动分类
- DELETE /api/admin/categories/{id}?cascade=true: 级联删除
- CategoryRequest 新增 parentId 和 sortOrder
"
```

---

### Task 13: 创建排序控制器

**文件：**
- 创建: `src/main/java/com/example/cms/presentation/controller/admin/AdminSortController.java`

**步骤 1: 创建控制器**

创建 `AdminSortController.java`：

```java
package com.example.cms.presentation.controller.admin;

import com.example.cms.application.service.SortOrderService;
import com.example.cms.presentation.dto.ApiResponse;
import com.example.cms.presentation.dto.ReorderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sort")
@RequiredArgsConstructor
public class AdminSortController {

    private final SortOrderService sortOrderService;

    @PutMapping("/reorder")
    public ApiResponse<Void> reorder(@RequestBody @Valid ReorderRequest request) {
        sortOrderService.reorder(request);
        return ApiResponse.success(null);
    }
}
```

**步骤 2: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 3: Commit**

```bash
git add src/main/java/com/example/cms/presentation/controller/admin/AdminSortController.java
git commit -m "feat: 添加排序控制器

- PUT /api/admin/sort/reorder: 批量更新排序
"
```

---

### Task 14: 扩展 ArticleService 支持分类变更

**文件：**
- 修改: `src/main/java/com/example/cms/application/service/ArticleService.java`

**步骤 1: 添加方法**

在 `ArticleService.java` 中添加方法：

```java
@Transactional
public void changeCategory(Long articleId, Long newCategoryId) {
    Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new IllegalArgumentException("文章不存在: " + articleId));

    // 验证新分类存在
    categoryRepository.findById(newCategoryId)
            .orElseThrow(() -> new IllegalArgumentException("目标分类不存在: " + newCategoryId));

    // 获取当前排序信息
    SortOrder currentSort = sortOrderService.getArticleSortOrder(articleId);
    int newSortOrder = 0;

    if (currentSort != null && currentSort.getParentId().equals(newCategoryId)) {
        // 同分类内排序，不改变序号
        return;
    }

    // 计算新分类下的排序序号
    List<SortOrder> siblings = sortOrderService.getChildrenByParent(
            com.example.cms.domain.model.sortorder.ResourceType.CATEGORY,
            newCategoryId
    );
    newSortOrder = siblings.size();

    // 更新文章分类
    article.changeCategory(newCategoryId);
    articleRepository.save(article);

    // 更新排序
    if (currentSort != null) {
        sortOrderService.moveArticleToCategory(articleId, newCategoryId, newSortOrder);
    } else {
        sortOrderService.initializeArticleSortOrder(articleId, newCategoryId, newSortOrder);
    }
}
```

**步骤 2: 扩展 SortOrderService**

在 `SortOrderService` 接口和实现类中添加方法：

```java
// 接口
void moveArticleToCategory(Long articleId, Long newCategoryId, int sortOrder);

// 实现
@Override
@Transactional
public void moveArticleToCategory(Long articleId, Long newCategoryId, int sortOrder) {
    SortOrder existing = sortOrderRepository
            .findByResourceTypeAndResourceId(ResourceType.ARTICLE, articleId)
            .orElse(null);

    if (existing != null) {
        SortOrder updated = SortOrder.builder()
                .id(existing.getId())
                .resourceType(ResourceType.ARTICLE)
                .resourceId(articleId)
                .parentType(ResourceType.CATEGORY)
                .parentId(newCategoryId)
                .sortOrder(sortOrder)
                .audit(existing.getAudit())
                .build();
        sortOrderRepository.save(updated);
    } else {
        initializeArticleSortOrder(articleId, newCategoryId, sortOrder);
    }
}
```

**步骤 3: 扩展 Article 实体**

在 `Article.java` 中添加方法：

```java
public void changeCategory(Long newCategoryId) {
    // 需要在 Article 实体中添加 categoryId 字段的 setter
    // 或者通过其他方式更新
}
```

**步骤 4: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL（可能需要调整 Article 实体）

**步骤 5: Commit**

```bash
git add src/main/java/com/example/cms/application/service/ArticleService.java
git add src/main/java/com/example/cms/application/service/SortOrderService.java
git add src/main/java/com/example/cms/application/service/SortOrderServiceImpl.java
git commit -m "feat: ArticleService 支持更改文章分类

- changeCategory: 将文章移动到新分类
- SortOrderService.moveArticleToCategory: 更新文章排序
"
```

---

### Task 15: 扩展 AdminArticleController

**文件：**
- 修改: `src/main/java/com/example/cms/presentation/controller/admin/AdminArticleController.java`

**步骤 1: 添加端点**

在 `AdminArticleController.java` 中添加方法：

```java
@PutMapping("/{id}/category")
public ApiResponse<Void> changeCategory(
        @PathVariable Long id,
        @RequestBody ChangeCategoryRequest request
) {
    articleService.changeCategory(id, request.getCategoryId());
    return ApiResponse.success(null);
}
```

**步骤 2: 创建 DTO**

创建 `ChangeCategoryRequest.java`：

```java
package com.example.cms.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeCategoryRequest {
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
}
```

**步骤 3: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 4: Commit**

```bash
git add src/main/java/com/example/cms/presentation/controller/admin/AdminArticleController.java
git add src/main/java/com/example/cms/presentation/dto/ChangeCategoryRequest.java
git commit -m "feat: AdminArticleController 支持更改文章分类

- PUT /api/admin/articles/{id}/category: 更改文章所属分类
"
```

---

### Task 16: 创建前台分类树 API

**文件：**
- 修改: `src/main/java/com/example/cms/presentation/controller/publicapi/PublicCategoryController.java`

**步骤 1: 添加端点**

在 `PublicCategoryController.java` 中添加方法：

```java
@GetMapping("/tree")
public ApiResponse<CategoryTreeResponse> getTree() {
    return ApiResponse.success(categoryService.getTree());
}
```

**步骤 2: 编译验证**

运行: `./gradlew compileJava`

预期: BUILD SUCCESSFUL

**步骤 3: Commit**

```bash
git add src/main/java/com/example/cms/presentation/controller/publicapi/PublicCategoryController.java
git commit -m "feat: PublicCategoryController 新增分类树端点

- GET /api/public/categories/tree: 获取前台分类树
"
```

---

## 阶段四：前端 - 增强 Markdown 编辑器

### Task 17: 安装 md-editor-v3

**文件：**
- 修改: `frontend/package.json`

**步骤 1: 进入前端目录安装依赖**

运行: `cd frontend && npm install md-editor-v3 --save`

预期: npm install 成功完成

**步骤 2: 验证安装**

检查 `frontend/package.json` 中包含 `md-editor-v3`

**步骤 3: Commit**

```bash
git add frontend/package.json frontend/package-lock.json
git commit -m "feat: 安装 md-editor-v3

- 增强 Markdown 编辑器库
- 支持 WYSIWYG + MD + 分屏预览
"
```

---

### Task 18: 升级 ArticleForm 组件

**文件：**
- 修改: `frontend/src/admin/views/ArticleForm.vue`

**步骤 1: 替换编辑器**

修改 `ArticleForm.vue`：

```vue
<template>
  <div style="max-width: 900px; margin: 0 auto; padding: 20px;">
    <h2>{{ isEdit ? '编辑文章' : '新建文章' }}</h2>

    <el-form :model="form" label-width="100px" style="margin-top: 20px">
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
        <MdEditor
          v-model="form.content"
          @onUploadImg="handleImageUpload"
          :toolbars="toolbars"
          style="height: 500px"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="save">{{ isEdit ? '更新' : '创建草稿' }}</el-button>
        <el-button @click="publish" v-if="!isEdit || form.status === 'DRAFT'">发布</el-button>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { articleApi } from '@/api/article'
import { categoryApi } from '@/api/category'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const id = computed(() => route.params.id)

const form = ref({
  title: '',
  content: '',
  categoryId: null,
  status: 'DRAFT'
})

const categories = ref([])

const toolbars = [
  'bold', 'underline', 'italic', 'strikeThrough',
  '-', 'title', 'sub', 'sup', 'quote',
  '-', 'unorderedList', 'orderedList', 'task',
  '-', 'codeRow', 'code',
  '-', 'link', 'image', 'table',
  '-', 'revoke', 'next', 'save',
  '=', 'pageFullscreen', 'fullscreen', 'preview', 'htmlPreview'
]

// 图片上传转 Base64
const handleImageUpload = async (files, callback) => {
  const base64Images = await Promise.all(
    files.map(file => fileToBase64(file))
  )
  callback(base64Images)
}

const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

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
    const res = await articleApi.adminGetAll()
    const article = res.data.find(a => a.id === Number(id.value))
    if (article) {
      form.value = {
        title: article.title,
        content: article.content,
        categoryId: article.categoryId,
        status: article.status
      }
    }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

const save = async () => {
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

const publish = async () => {
  try {
    const data = { ...form.value, status: 'PUBLISHED' }
    if (isEdit.value) {
      await articleApi.update(id.value, data)
    } else {
      await articleApi.create(data)
    }
    ElMessage.success('发布成功')
    router.push('/admin/articles')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '发布失败')
  }
}

onMounted(() => {
  loadCategories()
  load()
})
</script>
```

**步骤 2: 验证编译**

运行: `cd frontend && npm run build`

预期: 构建成功

**步骤 3: Commit**

```bash
git add frontend/src/admin/views/ArticleForm.vue
git commit -m "feat: ArticleForm 集成 md-editor-v3

- 替换为 md-editor-v3 增强编辑器
- 图片上传自动转 Base64 嵌入
- 支持工具栏快捷键
"
```

---

## 阶段五：前端 - 分类树组件

### Task 19: 创建前台分类侧边栏组件

**文件：**
- 创建: `frontend/src/public/components/CategorySidebar.vue`

**步骤 1: 创建组件**

创建 `CategorySidebar.vue`：

```vue
<template>
  <div class="category-sidebar">
    <div class="sidebar-title">分类目录</div>
    <el-tree
      :data="treeData"
      :props="treeProps"
      node-key="id"
      :default-expand-all="false"
      @node-click="handleNodeClick"
      class="category-tree"
    >
      <template #default="{ node, data }">
        <span class="tree-node">
          <span class="node-label">{{ data.name }}</span>
          <span class="node-count" v-if="data.articleCount !== undefined">
            ({{ data.articleCount }})
          </span>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi } from '@/api'

const router = useRouter()
const treeData = ref([])

const treeProps = {
  children: 'children',
  label: 'name'
}

const loadTree = async () => {
  try {
    const res = await publicApi.getCategoryTree()
    treeData.value = res.data.tree || []
  } catch (e) {
    console.error('加载分类树失败', e)
  }
}

const handleNodeClick = (data) => {
  router.push({ name: 'CategoryArticleList', params: { id: data.id } })
}

onMounted(loadTree)
</script>

<style scoped>
.category-sidebar {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
}

.category-tree {
  background: transparent;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}

.node-label {
  flex: 1;
}

.node-count {
  color: #999;
  font-size: 12px;
}

:deep(.el-tree-node__content) {
  height: 36px;
}

:deep(.el-tree-node__content:hover) {
  background-color: #f5f7fa;
}
</style>
```

**步骤 2: 编译验证**

运行: `cd frontend && npm run build`

预期: 构建成功

**步骤 3: Commit**

```bash
git add frontend/src/public/components/CategorySidebar.vue
git commit -m "feat: 添加前台分类侧边栏组件

- 可折叠树形导航
- 点击跳转到分类文章列表
"
```

---

### Task 20: 创建前台顶部导航栏组件

**文件：**
- 创建: `frontend/src/public/components/TopCategoryNav.vue`

**步骤 1: 创建组件**

创建 `TopCategoryNav.vue`：

```vue
<template>
  <nav class="top-nav">
    <div class="nav-container">
      <div class="nav-logo" @click="router.push('/')">
        <span>CMS</span>
      </div>
      <div class="nav-menu">
        <div
          v-for="category in rootCategories"
          :key="category.id"
          class="nav-item"
          @mouseenter="showDropdown(category.id)"
          @mouseleave="hideDropdown"
        >
          <span @click="goToCategory(category.id)">{{ category.name }}</span>
          <div v-if="category.children?.length" class="dropdown" :class="{ show: activeDropdown === category.id }">
            <div
              v-for="child in category.children"
              :key="child.id"
              class="dropdown-item"
              @click="goToCategory(child.id)"
            >
              {{ child.name }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi } from '@/api'

const router = useRouter()
const treeData = ref([])
const activeDropdown = ref(null)

const rootCategories = computed(() => {
  return treeData.value.filter(cat => !cat.parentId)
})

const loadTree = async () => {
  try {
    const res = await publicApi.getCategoryTree()
    treeData.value = res.data.tree || []
  } catch (e) {
    console.error('加载导航失败', e)
  }
}

const showDropdown = (id) => {
  activeDropdown.value = id
}

const hideDropdown = () => {
  activeDropdown.value = null
}

const goToCategory = (id) => {
  router.push({ name: 'CategoryArticleList', params: { id } })
  activeDropdown.value = null
}

onMounted(loadTree)
</script>

<style scoped>
.top-nav {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 60px;
  padding: 0 20px;
}

.nav-logo {
  font-size: 24px;
  font-weight: bold;
  margin-right: 40px;
  cursor: pointer;
}

.nav-menu {
  display: flex;
  gap: 8px;
}

.nav-item {
  position: relative;
  padding: 8px 16px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.nav-item:hover {
  background: #f5f7fa;
}

.dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  min-width: 150px;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-8px);
  transition: all 0.2s;
}

.dropdown.show {
  opacity: 1;
  visibility: visible;
  transform: translateY(4px);
}

.dropdown-item {
  padding: 10px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.dropdown-item:hover {
  background: #f5f7fa;
}
</style>
```

**步骤 2: 编译验证**

运行: `cd frontend && npm run build`

预期: 构建成功

**步骤 3: Commit**

```bash
git add frontend/src/public/components/TopCategoryNav.vue
git commit -m "feat: 添加前台顶部导航栏组件

- 一级分类展示
- 悬停显示子分类下拉菜单
"
```

---

### Task 21: 更新前台 Home 页面布局

**文件：**
- 修改: `frontend/src/public/views/Home.vue`

**步骤 1: 更新布局**

修改 `Home.vue`，引入侧边栏和导航栏：

```vue
<template>
  <div class="home-page">
    <TopCategoryNav />
    <div class="content-container">
      <CategorySidebar />
      <div class="main-content">
        <h1>最新文章</h1>
        <div class="article-list">
          <div
            v-for="article in articles"
            :key="article.id"
            class="article-item"
            @click="viewArticle(article.id)"
          >
            <h3>{{ article.title }}</h3>
            <p class="article-meta">
              <span>{{ getCategoryName(article.categoryId) }}</span>
              <span>{{ formatDate(article.publishedAt) }}</span>
            </p>
            <p class="article-excerpt">{{ getExcerpt(article.renderedContent) }}</p>
          </div>
        </div>
        <el-empty v-if="articles.length === 0" description="暂无文章" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopCategoryNav from '@/public/components/TopCategoryNav.vue'
import CategorySidebar from '@/public/components/CategorySidebar.vue'
import { publicApi } from '@/api'

const router = useRouter()
const articles = ref([])
const categories = ref([])

const loadArticles = async () => {
  try {
    const res = await publicApi.getPublishedArticles()
    articles.value = res.data
  } catch (e) {
    console.error('加载文章失败', e)
  }
}

const loadCategories = async () => {
  try {
    const res = await publicApi.getCategories()
    categories.value = res.data
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

const getCategoryName = (categoryId) => {
  const cat = categories.value.find(c => c.id === categoryId)
  return cat?.name || '未知'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const getExcerpt = (html) => {
  if (!html) return ''
  const text = html.replace(/<[^>]+>/g, '')
  return text.length > 100 ? text.substring(0, 100) + '...' : text
}

const viewArticle = (id) => {
  router.push({ name: 'ArticleDetail', params: { id } })
}

onMounted(() => {
  loadArticles()
  loadCategories()
})
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.content-container {
  max-width: 1200px;
  margin: 20px auto;
  display: flex;
  gap: 20px;
  padding: 0 20px;
}

.main-content {
  flex: 1;
  background: #fff;
  border-radius: 4px;
  padding: 20px;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.article-item {
  padding: 16px;
  border: 1px solid #eee;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.article-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-color: #409eff;
}

.article-item h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.article-meta {
  color: #999;
  font-size: 14px;
  display: flex;
  gap: 16px;
}

.article-excerpt {
  color: #666;
  margin: 8px 0 0;
}
</style>
```

**步骤 2: 编译验证**

运行: `cd frontend && npm run build`

预期: 构建成功

**步骤 3: Commit**

```bash
git add frontend/src/public/views/Home.vue
git commit -m "feat: Home 页面集成导航栏和侧边栏

- 顶部导航栏
- 左侧分类侧边栏
- 右侧文章列表
"
```

---

## 阶段六：前端 - 后台分类树拖拽

### Task 22: 升级后台分类树组件支持拖拽

**文件：**
- 创建: `frontend/src/admin/components/CategoryTree.vue`

**步骤 1: 创建可拖拽分类树组件**

创建 `CategoryTree.vue`：

```vue
<template>
  <div class="category-tree-container">
    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="treeProps"
      node-key="id"
      default-expand-all
      draggable
      :allow-drag="allowDrag"
      :allow-drop="allowDrop"
      @node-drop="handleNodeDrop"
      class="category-tree"
    >
      <template #default="{ node, data }">
        <span class="tree-node">
          <span class="node-label">{{ data.name }}</span>
          <span class="node-actions">
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="editCategory(data)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click.stop="deleteCategory(data)"
            >
              删除
            </el-button>
          </span>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '@/api/category'

const emit = defineEmits(['edit', 'refresh'])

const treeRef = ref()
const treeData = ref([])

const treeProps = {
  children: 'children',
  label: 'name'
}

const allowDrag = (node) => {
  return true
}

const allowDrop = (draggingNode, dropNode, type) => {
  // 不允许拖拽到自己的子节点
  if (type === 'inner') {
    return !isDescendant(draggingNode.data.id, dropNode.data.id)
  }
  return true
}

const isDescendant = (ancestorId, nodeId) => {
  const node = findNode(treeData.value, nodeId)
  if (!node) return false

  const check = (n) => {
    if (n.id === ancestorId) return true
    if (n.children) {
      return n.children.some(child => check(child))
    }
    return false
  }

  return check(node)
}

const findNode = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children) {
      const found = findNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

const handleNodeDrop = async (draggingNode, dropNode, dropType) => {
  const draggedId = draggingNode.data.id
  const targetId = dropNode.data.id

  try {
    if (dropType === 'inner') {
      // 移动到目标节点下作为子节点
      await categoryApi.moveToCategory(draggedId, targetId)
    } else {
      // 移动到目标节点的同级
      const parentId = dropNode.parent?.data?.id || null
      await categoryApi.moveToCategory(draggedId, parentId)

      // 重新排序
      await reorderSiblings(parentId, dropNode.parent.childNodes)
    }

    ElMessage.success('移动成功')
    emit('refresh')
    loadTree()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '移动失败')
    loadTree() // 恢复原状
  }
}

const reorderSiblings = async (parentId, nodes) => {
  const sortedIds = nodes.map(n => n.data.id)
  await categoryApi.reorder({
    parentType: 'CATEGORY',
    parentId: parentId,
    items: sortedIds.map((id, index) => ({
      resourceType: 'CATEGORY',
      resourceId: id,
      sortOrder: index
    }))
  })
}

const editCategory = (data) => {
  emit('edit', data)
}

const deleteCategory = async (data) => {
  try {
    await ElMessageBox.confirm(
      data.children?.length
        ? '该分类下有子分类，是否确认删除？'
        : '确认删除该分类？',
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const cascade = !!data.children?.length
    await categoryApi.delete(data.id, cascade)
    ElMessage.success('删除成功')
    emit('refresh')
    loadTree()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

const loadTree = async () => {
  try {
    const res = await categoryApi.getTree()
    treeData.value = res.data.tree || []
  } catch (e) {
    ElMessage.error('加载分类树失败')
  }
}

defineExpose({ loadTree })

onMounted(loadTree)
</script>

<style scoped>
.category-tree-container {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
}

.category-tree {
  background: transparent;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 8px;
}

.node-label {
  flex: 1;
}

.node-actions {
  opacity: 0;
  transition: opacity 0.2s;
}

:deep(.el-tree-node__content:hover) .node-actions {
  opacity: 1;
}

:deep(.el-tree-node__content) {
  height: 40px;
}
</style>
```

**步骤 2: 更新 API 模块**

修改 `frontend/src/api/category.js`，添加新方法：

```javascript
export const categoryApi = {
  // ... 保留现有方法 ...

  getTree: () => request.get('/api/admin/categories/tree'),

  moveToCategory: (id, newParentId) =>
    request.put(`/api/admin/categories/${id}/move`, { newParentId }),

  delete: (id, cascade = false) =>
    request.delete(`/api/admin/categories/${id}`, { params: { cascade } }),

  reorder: (data) => request.put('/api/admin/sort/reorder', data)
}
```

**步骤 3: 编译验证**

运行: `cd frontend && npm run build`

预期: 构建成功

**步骤 4: Commit**

```bash
git add frontend/src/admin/components/CategoryTree.vue
git add frontend/src/api/category.js
git commit -m "feat: 后台分类树支持拖拽

- 节点拖拽移动（跨级/同级）
- 拖拽后自动重排
- 防止循环引用
- 级联删除确认
"
```

---

### Task 23: 更新 CategoryList 集成树形组件

**文件：**
- 修改: `frontend/src/admin/views/CategoryList.vue`

**步骤 1: 更新页面**

修改 `CategoryList.vue`：

```vue
<template>
  <div class="category-list">
    <div class="page-header">
      <h2>分类管理</h2>
      <el-button type="primary" @click="showCreateDialog">新建分类</el-button>
    </div>

    <CategoryTree ref="treeRef" @edit="handleEdit" @refresh="handleRefresh" />

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '新建分类'"
      width="500px"
    >
      <el-form :model="form" label-width="100px">
        <el-form-item label="父分类">
          <el-tree-select
            v-model="form.parentId"
            :data="treeData"
            :props="{ label: 'name', value: 'id' }"
            placeholder="选择父分类（留空为根分类）"
            check-strictly
            clearable
          />
        </el-form-item>
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
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import CategoryTree from '@/admin/components/CategoryTree.vue'
import { categoryApi } from '@/api/category'

const treeRef = ref()
const treeData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref({
  id: null,
  parentId: null,
  name: '',
  description: '',
  sortOrder: 0
})

const showCreateDialog = () => {
  isEdit.value = false
  form.value = {
    id: null,
    parentId: null,
    name: '',
    description: '',
    sortOrder: 0
  }
  dialogVisible.value = true
}

const handleEdit = (data) => {
  isEdit.value = true
  form.value = {
    id: data.id,
    parentId: null, // 这里需要从 sort_orders 获取
    name: data.name,
    description: data.description,
    sortOrder: data.sortOrder || 0
  }
  dialogVisible.value = true
}

const handleRefresh = () => {
  treeRef.value?.loadTree()
}

const save = async () => {
  try {
    if (isEdit.value) {
      await categoryApi.update(form.value.id, {
        name: form.value.name,
        description: form.value.description
      })
    } else {
      await categoryApi.create({
        name: form.value.name,
        description: form.value.description,
        parentId: form.value.parentId,
        sortOrder: form.value.sortOrder
      })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    treeRef.value?.loadTree()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  }
}
</script>

<style scoped>
.category-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
```

**步骤 2: 编译验证**

运行: `cd frontend && npm run build`

预期: 构建成功

**步骤 3: Commit**

```bash
git add frontend/src/admin/views/CategoryList.vue
git commit -m "feat: CategoryList 集成树形组件

- 使用 CategoryTree 替换列表视图
- 支持拖拽排序
- 支持移动到不同父分类
"
```

---

## 阶段七：前端 - API 更新

### Task 24: 更新前台 API 模块

**文件：**
- 修改: `frontend/src/api/index.js` 或 `frontend/src/api/public.js`

**步骤 1: 添加分类树 API**

在公共 API 模块中添加：

```javascript
// frontend/src/api/index.js 或新建 frontend/src/api/public.js

export const publicApi = {
  // 获取分类树
  getCategoryTree: () => request.get('/api/public/categories/tree'),

  // 获取已发布文章列表
  getPublishedArticles: () => request.get('/api/public/articles'),

  // 获取分类列表
  getCategories: () => request.get('/api/public/categories')
}
```

**步骤 2: 编译验证**

运行: `cd frontend && npm run build`

预期: 构建成功

**步骤 3: Commit**

```bash
git add frontend/src/api/
git commit -m "feat: 前台 API 模块更新

- 添加 getCategoryTree
- 添加 getPublishedArticles
"
```

---

## 阶段八：集成测试

### Task 25: 端到端测试

**文件：**
- 创建: `frontend/tests/e2e/category.spec.js` (可选)

**步骤 1: 启动完整应用测试**

运行: `./gradlew bootRun`

预期: 应用成功启动

**步骤 2: 手动测试清单**

- [ ] 后台创建根分类
- [ ] 后台创建子分类
- [ ] 后台拖拽分类改变层级
- [ ] 后台拖拽分类改变排序
- [ ] 后台创建文章并上传图片（Base64）
- [ ] 后台拖拽文章到不同分类
- [ ] 前台查看分类树
- [ ] 前台点击分类筛选文章
- [ ] 前台顶部导航栏下拉菜单

**步骤 3: 修复发现的问题**

根据测试结果修复 bug

**步骤 4: Commit**

```bash
git add -A
git commit -m "test: 完成端到端测试

- 验证分类树创建、拖拽、排序
- 验证文章编辑和图片上传
- 验证前台导航和筛选
"
```

---

## 最终步骤

### Task 26: 构建和部署

**步骤 1: 完整构建**

运行: `./gradlew clean build`

预期: BUILD SUCCESSFUL

**步骤 2: 复制前端资源**

运行: `./build-frontend.sh`

预期: 前端资源成功复制到 `src/main/resources/static/`

**步骤 3: 最终验证**

运行: `./gradlew bootRun`

访问: `http://localhost:8080`

**步骤 4: 最终 Commit**

```bash
git add -A
git commit -m "chore: 完成迭代1实现

- 多级分类树结构
- 通用排序表 sort_orders
- 增强 Markdown 编辑器 (md-editor-v3)
- 前台顶部导航 + 侧边栏树
- 后台拖拽排序功能
"
```

---

**计划完成！总计 26 个任务。**

使用 `superpowers:executing-plans` 开始逐步执行此计划。
