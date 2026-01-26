# 迭代1：多级分类与富文本增强 - 设计文档

**日期**：2025-01-27
**状态**：待实现

---

## 1. 需求概述

| 需求 | 实现方式 |
|------|----------|
| 前台多级分类展示 | 左侧可折叠分类树 + 右侧文章列表 |
| 后台编辑器 | 增强 MD 编辑器（WYSIWYG + MD快捷键 + 实时预览） |
| 图片上传 | Base64 编码直接嵌入 MD 内容 |
| 前台导航栏 | 顶部固定导航栏（一级分类 + 子分类下拉） |
| 分类拖拽 | 移动节点、同级排序、防循环引用、级联删除 |
| 文章拖拽 | 拖拽文章到分类改变所属分类 |

---

## 2. 数据模型设计

### 2.1 新增 SortOrder 实体

```java
public class SortOrder {
    private final Long id;
    private final ResourceType resourceType;  // CATEGORY, ARTICLE
    private final Long resourceId;
    private final ResourceType parentType;    // CATEGORY (分类的父) 或 null
    private final Long parentId;              // 父ID
    private int sortOrder;
    private Audit audit;

    public enum ResourceType {
        CATEGORY, ARTICLE
    }

    public void updateOrder(int newSortOrder) {
        this.sortOrder = newSortOrder;
        this.audit = this.audit.markModified();
    }
}
```

### 2.2 数据库迁移（Flyway V3）

```sql
-- 通用排序表（唯一的层级+排序存储）
CREATE TABLE sort_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_type VARCHAR(20) NOT NULL,  -- 'CATEGORY' or 'ARTICLE'
    resource_id BIGINT NOT NULL,
    parent_type VARCHAR(20),              -- 'CATEGORY' or NULL
    parent_id BIGINT,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_resource (resource_type, resource_id),
    INDEX idx_parent (parent_type, parent_id)
);
```

**设计说明**：
- `categories` 和 `articles` 表本身保持纯粹，只存储业务属性
- 层级关系（父子）和排序完全由 `sort_orders` 统一管理
- 分类和文章复用同一套排序机制

### 2.3 查询示例

```sql
-- 获取某分类下的所有子分类（按排序）
SELECT c.*, so.sort_order
FROM categories c
INNER JOIN sort_orders so
  ON so.resource_type = 'CATEGORY'
  AND so.resource_id = c.id
WHERE so.parent_type = 'CATEGORY'
  AND so.parent_id = ?
ORDER BY so.sort_order;

-- 获取某分类下的所有文章（按排序）
SELECT a.*, so.sort_order
FROM articles a
INNER JOIN sort_orders so
  ON so.resource_type = 'ARTICLE'
  AND so.resource_id = a.id
WHERE so.parent_type = 'CATEGORY'
  AND so.parent_id = ?
ORDER BY so.sort_order;
```

---

## 3. 后端 API 设计

### 3.1 统一排序服务（SortOrderService）

```java
public interface SortOrderService {
    // 批量更新排序
    void reorder(ReorderRequest request);
}

public class ReorderRequest {
    private ResourceType parentType;  // CATEGORY
    private Long parentId;            // 父ID，null 表示根级
    private List<OrderItem> items;    // 排序项列表
}

public class OrderItem {
    private ResourceType resourceType;  // CATEGORY or ARTICLE
    private Long resourceId;
    private int sortOrder;              // 新序号（0-based）
}
```

**实现说明**：使用 `CASE WHEN` 批量更新
```sql
UPDATE sort_orders
SET sort_order = CASE resource_id
    WHEN 5 THEN 0
    WHEN 3 THEN 1
    WHEN 8 THEN 2
END
WHERE resource_type = ?
  AND parent_type = ?
  AND parent_id = ?
  AND resource_id IN (5, 3, 8);
```

### 3.2 分类服务（CategoryService）新增

```java
public interface CategoryService {
    // 获取完整分类树（递归构建）
    CategoryTreeResponse getTree();

    // 移动分类节点（改变父分类）
    void moveToCategory(Long categoryId, Long newParentId);

    // 级联删除分类
    void deleteCategory(Long categoryId, boolean cascade);
}
```

**防循环引用**：
```java
private void validateNoCycle(Long categoryId, Long targetParentId) {
    if (targetParentId == null) return;
    Set<Long> descendants = getAllDescendants(targetParentId);
    if (descendants.contains(categoryId)) {
        throw new BusinessException("不能将分类移动到其子分类下");
    }
}
```

### 3.3 文章服务（ArticleService）新增

```java
public interface ArticleService {
    // 改变文章分类
    void changeCategory(Long articleId, Long newCategoryId);
}
```

### 3.4 REST API 端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/public/categories/tree` | 前台：获取分类树 |
| GET | `/api/admin/categories/tree` | 后台：获取分类树（含操作权限） |
| PUT | `/api/admin/sort/reorder` | 通用：批量排序 |
| PUT | `/api/admin/categories/{id}/move` | 移动分类节点 |
| DELETE | `/api/admin/categories/{id}` | 删除分类（含 cascade 参数） |
| PUT | `/api/admin/articles/{id}/category` | 改变文章分类 |

---

## 4. 前端组件设计

### 4.1 新增/升级组件

| 组件 | 职责 | 关键交互 |
|------|------|----------|
| `TopCategoryNav.vue` | 顶部导航栏 | 展示一级分类，悬停显示子分类下拉菜单 |
| `CategorySidebar.vue` | 左侧分类树（前台） | 可折叠树形，点击筛选右侧文章 |
| `CategoryTree.vue`（升级） | 后台分类管理树 | 拖拽节点移动、同级排序、右键菜单 |
| `ArticleForm.vue`（升级） | 文章编辑 | 集成增强 MD 编辑器 + 图片 Base64 处理 |
| `CategoryForm.vue` | 分类编辑表单 | 父分类选择（树形下拉）、同级排序输入 |

### 4.2 拖拽交互设计（后台）

**分类节点拖拽**：
- 同级拖拽 → `PUT /api/admin/sort/reorder`
- 跨级拖拽 → `PUT /api/admin/categories/{id}/move` + `reorder`（新旧父分类）

**文章拖拽到分类**：
- 文章卡片 → 分类树节点 → `PUT /api/admin/articles/{id}/category`

### 4.3 增强 MD 编辑器

**选型**：`md-editor-v3`（Vue 3 专用）

特性：
- WYSIWYG + MD 源码 + 分屏预览三模式切换
- 内置图片上传（可拦截转为 Base64）
- 工具栏快捷键、表格、代码高亮

```vue
<template>
  <MdEditor
    v-model="content"
    @onUploadImg="handleImageUpload"
    :toolbars="toolbars"
  />
</template>

<script setup>
const handleImageUpload = async (files, callback) => {
  const base64Images = await Promise.all(
    files.map(file => fileToBase64(file))
  );
  callback(base64Images);
};

const fileToBase64 = (file) => {
  return new Promise((resolve) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.readAsDataURL(file);
  });
};
</script>
```

---

## 5. 领域模型变更

### 5.1 SortOrder 聚合

```
SortOrder (聚合根)
├── resourceType: ResourceType
├── resourceId: Long
├── parentType: ResourceType
├── parentId: Long
├── sortOrder: int
└── audit: Audit
```

### 5.2 Category 聚合（无变更）

Category 实体本身保持不变，层级关系通过 SortOrder 表维护。

---

## 6. 实现计划

1. **数据库迁移**：创建 `sort_orders` 表
2. **后端**：
   - 实现 SortOrder 实体和仓储
   - 实现 SortOrderService（批量排序）
   - CategoryService 增加 tree/move/delete 方法
   - ArticleService 增加 changeCategory 方法
   - 添加 REST Controller
3. **前端**：
   - 集成 md-editor-v3
   - 实现 TopCategoryNav.vue
   - 实现 CategorySidebar.vue
   - 升级 CategoryTree.vue 支持拖拽
   - 升级 ArticleForm.vue 支持图片 Base64
4. **测试**：
   - 单元测试：排序逻辑、循环引用检测
   - 集成测试：拖拽交互、图片上传
