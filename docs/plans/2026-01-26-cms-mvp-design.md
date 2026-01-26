# CMS MVP 系统设计文档

**日期**：2026-01-26
**版本**：v1.0

---

## 1. 整体架构

系统采用前后端一体化部署架构，前端构建产物直接打包到 Spring Boot 的 `static` 目录，由单一应用进程提供服务。

### 技术栈

| 层级 | 技术选型 |
|------|----------|
| 前端 | Vue 3 + Element Plus + Vue Router |
| 后端 | Java + Spring Boot + JPA |
| 数据库 | H2（开发环境）+ Flyway |
| 构建部署 | npm build → static 目录 → 单进程启动 |

### 系统分层

- **前台展示端**：公开访问，提供内容列表和详情页
- **后台管理端**：无认证直接访问，提供分类和内容的 CRUD 操作
- **后端 DDD 分层**：Presentation → Application → Domain → Infrastructure

---

## 2. 领域模型

### Category（分类）- 聚合根

```
- id: Long (唯一标识)
- name: String (分类名称，不可变)
- description: String (可选)
- createdAt: LocalDateTime
- updatedAt: LocalDateTime

方法：
- rename(name)
- updateDescription(description)
```

### Article（文章）- 聚合根

```
- id: Long (唯一标识)
- title: String
- content: String (原始 Markdown)
- renderedContent: String (预渲染的 HTML)
- status: ArticleStatus (DRAFT / PUBLISHED)
- categoryId: Long (关联分类 ID)
- publishedAt: LocalDateTime (可选)
- createdAt: LocalDateTime
- updatedAt: LocalDateTime

方法：
- updateContent(content)  // 自动触发 MD→HTML 渲染
- publish()
- unpublish()
- changeCategory(categoryId)
```

### 值对象

- `ArticleStatus`：枚举，`DRAFT`、`PUBLISHED`
- `RenderedContent`：封装 HTML 渲染逻辑，确保格式安全

### 领域关系

```
Category 1 ←──→ N Article
(分类下可有多个文章，文章只能属于一个分类)
```

---

## 3. API 设计

### 后台管理 API (`/api/admin/*`)

#### 分类管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/categories` | 获取所有分类 |
| POST | `/api/admin/categories` | 创建分类 |
| PUT | `/api/admin/categories/{id}` | 更新分类 |
| DELETE | `/api/admin/categories/{id}` | 删除分类（需检查关联文章） |

#### 内容管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/admin/articles` | 获取所有文章（分页、筛选） |
| GET | `/api/admin/articles/{id}` | 获取文章详情 |
| POST | `/api/admin/articles` | 创建文章（自动渲染 MD→HTML） |
| PUT | `/api/admin/articles/{id}` | 更新文章 |
| DELETE | `/api/admin/articles/{id}` | 删除文章 |
| POST | `/api/admin/articles/{id}/publish` | 发布文章 |
| POST | `/api/admin/articles/{id}/unpublish` | 取消发布 |

### 前台展示 API (`/api/public/*`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/public/categories` | 获取所有分类 |
| GET | `/api/public/articles` | 获取已发布文章列表（按发布时间倒序、支持分类筛选） |
| GET | `/api/public/articles/{id}` | 获取已发布文章详情 |

### 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

---

## 4. 前端页面设计

### 后台管理端 (`/admin/*`)

| 路由 | 页面 | 说明 |
|------|------|------|
| `/admin` | 首页 | 功能入口导航 |
| `/admin/categories` | 分类列表 | 表格展示 + 新建/编辑/删除 |
| `/admin/categories/new` | 新建分类 | 表单 |
| `/admin/categories/:id/edit` | 编辑分类 | 表单 |
| `/admin/articles` | 文章列表 | 表格展示 + 筛选 + 新建/编辑/删除/发布 |
| `/admin/articles/new` | 新建文章 | 表单 + textarea 编辑器 |
| `/admin/articles/:id/edit` | 编辑文章 | 表单 + textarea 编辑器 |

### 前台展示端 (`/*`)

| 路由 | 页面 | 说明 |
|------|------|------|
| `/` | 首页 | 文章列表（按发布时间倒序） |
| `/category/:id` | 分类文章 | 某分类下的文章列表 |
| `/article/:id` | 文章详情 | 渲染 HTML 内容 |

### 组件结构

```
├── AdminLayout          # 后台布局（侧边栏导航）
├── CategoryList         # 分类列表
├── CategoryForm         # 分类表单
├── ArticleList          # 文章列表
├── ArticleForm          # 文章表单（textarea 编辑器）
├── PublicLayout         # 前台布局（顶部导航）
├── PublicArticleList    # 前台文章列表（卡片式）
└── ArticleDetail        # 文章详情（HTML 渲染）
```

---

## 5. 核心流程

### Markdown 渲染流程

```
1. 用户创建/更新文章
2. Service 层调用 MarkdownRenderer
3. 使用 flexmark-java 将 MD 转换为 HTML
4. sanitize HTML（防 XSS）
5. 存入 renderedContent 字段
6. 前台读取直接渲染，无需实时转换
```

### 文章发布流程

```
1. 创建文章 → status = DRAFT
2. 编辑内容保存 → 自动重新渲染 HTML
3. 点击发布 → status = PUBLISHED, 记录 publishedAt
4. 前台只能查询 PUBLISHED 状态的文章
```

### 删除分类流程

```
1. 检查是否有关联文章
2. 有 → 返回错误，提示先处理文章
3. 无 → 允许删除
```

---

## 6. 项目结构

### 后端包结构

```
com.example.cms/
├── presentation/          # 展示层
│   ├── controller/
│   │   ├── admin/         # 后台 API
│   │   └── public/        # 前台 API
│   └── dto/               # 请求/响应 DTO
├── application/           # 应用层
│   └── service/           # Application Service
├── domain/                # 领域层
│   ├── model/
│   │   ├── category/      # Category 聚合
│   │   └── article/       # Article 聚合
│   ├── repository/        # Repository 接口
│   └── service/           # 领域服务（MarkdownRenderer）
├── infrastructure/        # 基础设施层
│   ├── persistence/
│   │   ├── entity/        # JPA Entity
│   │   └── repository/    # Repository 实现
│   └── flyway/            # 数据库迁移脚本
└── config/                # 配置类
```

### 前端目录结构

```
frontend/
├── src/
│   ├── admin/             # 后台代码
│   │   ├── components/
│   │   ├── views/
│   │   └── router.js
│   ├── public/            # 前台代码
│   │   ├── components/
│   │   ├── views/
│   │   └── router.js
│   ├── shared/            # 共用组件/工具
│   └── main.js
└── package.json
```

---

## 7. 设计原则

- **YAGNI**：MVP 只实现核心功能，暂不考虑用户认证、评论、标签等
- **充血模型**：领域对象封装业务逻辑，而非贫血 POJO
- **SOLID 原则**：职责单一、开放封闭、里氏替换、接口隔离、依赖倒置
- **聚合边界清晰**：Category 和 Article 各自独立，通过 ID 关联
- **预渲染优化**：保存时渲染，前台直接输出，避免实时计算
