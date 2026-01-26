# CMS MVP Design Document

**Date**: 2025-01-26
**Status**: Approved

## Overview

A Content Management System (CMS) with dual interfaces:
- **Public**: Browse and read published content
- **Admin**: Manage categories and create/edit Markdown content

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Vue 3 + Ant Design Vue + Vite |
| Backend | Java 17+ + Spring Boot 3.x + Spring Data JPA |
| Database | MySQL (prod) / H2 (dev) + Flyway |
| MD Rendering | flexmark-java (server-side) |

## Project Structure

```
cms-system/
├── frontend/              # Vue 3 application
│   ├── src/
│   │   ├── admin/        # Admin interface
│   │   ├── public/       # Public site
│   │   ├── shared/       # Shared code
│   │   └── main.ts
│   └── vite.config.ts
├── backend/
│   └── src/main/
│       ├── java/
│       │   ├── presentation/  # Controllers, DTOs
│       │   ├── application/   # Services
│       │   ├── domain/        # Entities, VOs
│       │   └── infrastructure/# Repositories
│       └── resources/
│           ├── db/migration/
│           └── static/        # Built frontend
└── docs/
```

## Domain Model

### Entities

**Category** (Aggregate Root)
```
- id: CategoryId
- name: String
- slug: String
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

**Content** (Aggregate Root)
```
- id: ContentId
- title: String
- rawMarkdown: String
- renderedHtml: String
- status: PublishStatus (DRAFT, PUBLISHED)
- categoryId: CategoryId
- publishedAt: LocalDateTime
- createdAt: LocalDateTime
- updatedAt: LocalDateTime
```

### Domain Behaviors
- `Content.publish()`: Validate and transition to PUBLISHED
- `Content.renderMarkdown()`: Convert MD to HTML using flexmark
- `Content.updateContent()`: Handle MD→HTML conversion on save

## API Design

### Public API (No Auth)
```
GET  /api/public/contents          # List published contents
GET  /api/public/contents/{id}     # Get content detail
GET  /api/public/categories        # List categories
```

### Admin API (Basic Auth - MVP)
```
# Categories
GET    /api/admin/categories
POST   /api/admin/categories
PUT    /api/admin/categories/{id}
DELETE /api/admin/categories/{id}

# Contents
GET    /api/admin/contents
POST   /api/admin/contents
PUT    /api/admin/contents/{id}
DELETE /api/admin/contents/{id}
POST   /api/admin/contents/{id}/publish
POST   /api/admin/contents/{id}/unpublish
```

## Frontend Architecture

### Admin Pages
- `Dashboard.vue`: Overview
- `ContentList.vue`: Content management
- `ContentForm.vue`: Create/edit with MD editor
- `CategoryManage.vue`: Category CRUD
- `MdEditor.vue`: Markdown editor with preview

### Public Pages
- `Home.vue`: Content list by publish time
- `ContentDetail.vue`: Rendered content view
- `ContentCard.vue`: Content preview card

## Build & Deployment

### Development
```bash
# Frontend dev
cd frontend && npm run dev

# Backend dev
cd backend && ./mvnw spring-boot:run
```

### Production Build
```bash
# 1. Build frontend
cd frontend && npm run build

# 2. Copy to backend
cp -r dist/* ../backend/src/main/resources/static/

# 3. Build JAR
cd ../backend && ./mvnw clean package

# 4. Run
java -jar target/cms-*.jar
```

### Deployment Architecture
- Single JAR serves both frontend and backend
- Spring Boot serves static assets from `/static`
- Vue Router handles client-side routing
- Spring MVC fallback routes non-API requests to `index.html`

## Data Flow

### Content Creation
1. User creates content in Admin (MdEditor)
2. Frontend sends: `{ title, categoryId, rawMarkdown }`
3. Backend `ContentService.createContent()`
4. Domain: `Content.renderMarkdown()` converts MD → HTML
5. Both `rawMarkdown` and `renderedHtml` saved to DB
6. Response includes rendered HTML for preview

### Public Display
1. `GET /api/public/contents` returns list
2. User clicks content → `GET /api/public/contents/{id}`
3. Returns pre-rendered HTML directly
4. Frontend displays via `v-html`

## Error Handling

### Validation Layers
1. Frontend: Ant Design form validation
2. Controller: `@Valid` on DTOs
3. Domain: Business rule validation
4. Repository: Constraint validation

### Error Response Format
```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": [{"field": "title", "message": "required"}]
}
```

### Security (MVP)
- Admin API: Basic Auth (hardcoded credential)
- Public API: No auth
- MD sanitization: XSS prevention

## Testing Strategy

### Backend Tests
- Domain: Unit tests for business logic (80%+ coverage)
- Repository: Integration tests with Testcontainers
- Controller: Key endpoint tests

### Frontend Tests
- Component: Vitest for core components
- E2E: Playwright (optional for MVP)

### Test Data
- Flyway: `V1__test_data.sql`
- Factory pattern for test entities

## Future Iterations (Already Planned)

1. **Iteration 1**: Multi-level categories, rich text editor, category navigation
2. **Iteration 2**: User management, permissions, content search
3. **Iteration 3**: Comments, analytics, scheduled publishing
