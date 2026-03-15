# API 文档 - bd_dw_platform

## 概览

基础 URL：`http://localhost:8080`

所有 API 路径前缀：`/api/v1`

完整交互式文档：`http://localhost:8080/swagger-ui.html`

## 认证

系统使用 JWT Bearer Token 认证。

### 获取 Token

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

响应：
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

### 使用 Token

在请求头中携带：
```http
Authorization: Bearer <accessToken>
```

## API 端点列表

### 认证模块 `/api/v1/auth`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/login` | 用户登录 | 公开 |
| POST | `/logout` | 用户登出 | 已登录 |
| POST | `/refresh` | 刷新 Token | 已登录 |
| GET | `/me` | 获取当前用户信息 | 已登录 |

### 表元数据 `/api/v1/tables`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/` | 分页查询表列表 | 所有角色 |
| POST | `/` | 创建表元数据 | DEVELOPER/ADMIN |
| GET | `/{id}` | 获取表详情 | 所有角色 |
| PUT | `/{id}` | 更新表元数据 | DEVELOPER/ADMIN |
| DELETE | `/{id}` | 删除表元数据 | ADMIN |
| GET | `/{id}/columns` | 获取表的字段列表 | 所有角色 |

### 字段元数据 `/api/v1/columns`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/` | 创建字段 | DEVELOPER/ADMIN |
| PUT | `/{id}` | 更新字段 | DEVELOPER/ADMIN |
| DELETE | `/{id}` | 删除字段 | ADMIN |
| PUT | `/reorder` | 批量调整字段顺序 | DEVELOPER/ADMIN |

### 血缘关系 `/api/v1/lineage`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/` | 创建血缘关系 | DEVELOPER/ADMIN |
| DELETE | `/{id}` | 删除血缘关系 | ADMIN |
| GET | `/upstream/{tableId}` | 获取上游表 | 所有角色 |
| GET | `/downstream/{tableId}` | 获取下游表 | 所有角色 |
| GET | `/graph/{tableId}` | 获取血缘图谱 | 所有角色 |
| POST | `/impact` | 影响分析 | 所有角色 |
| POST | `/parse-sql` | SQL 解析提取血缘 | DEVELOPER/ADMIN |

### 搜索 `/api/v1/search`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/` | 全文搜索 | 所有角色 |
| GET | `/suggest` | 搜索建议 | 所有角色 |
| POST | `/filter` | 高级过滤 | 所有角色 |

### 数据目录 `/api/v1/catalogs`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/tree` | 获取目录树 | 所有角色 |
| GET | `/flat` | 获取目录扁平列表（供下拉选择） | 所有角色 |
| POST | `/` | 创建目录节点 | ADMIN |
| PUT | `/{id}` | 更新目录节点 | ADMIN |
| DELETE | `/{id}` | 删除目录节点 | ADMIN |
| POST | `/{id}/tables/{tableId}` | 添加表到目录 | DEVELOPER/ADMIN |
| DELETE | `/{id}/tables/{tableId}` | 从目录移除表 | DEVELOPER/ADMIN |

### 数据质量 `/api/v1/quality`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/{tableId}` | 获取最新质量指标 | 所有角色 |
| POST | `/` | 记录质量指标 | DEVELOPER/ADMIN |
| GET | `/{tableId}/trend` | 获取质量趋势 | 所有角色 |

### 变更历史 `/api/v1/history`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| GET | `/` | 全量查询变更历史（支持分页） | 所有角色 |
| GET | `/entity/{entityType}/{entityId}` | 获取实体变更历史 | 所有角色 |
| GET | `/user/{username}` | 获取用户操作历史 | ADMIN |

### 导入导出 `/api/v1/import-export`

| 方法 | 路径 | 描述 | 权限 |
|------|------|------|------|
| POST | `/import` | 导入元数据 | DEVELOPER/ADMIN |
| POST | `/export` | 创建导出任务 | 所有角色 |
| GET | `/export/{taskId}/status` | 查询导出状态 | 所有角色 |
| GET | `/export/{taskId}/download` | 下载导出文件 | 所有角色 |

## 通用响应格式

### 成功响应

```json
{
  "data": { ... },
  "message": "操作成功"
}
```

### 分页响应

```json
{
  "items": [ ... ],
  "total": 100,
  "page": 1,
  "pageSize": 20,
  "totalPages": 5
}
```

### 错误响应

```json
{
  "errorCode": "RESOURCE_NOT_FOUND",
  "errorMessage": "表元数据不存在",
  "details": "tableId: xxx",
  "timestamp": "2024-01-01T12:00:00",
  "requestId": "uuid"
}
```

## 错误码说明

| HTTP 状态码 | 错误码 | 说明 |
|------------|--------|------|
| 400 | VALIDATION_ERROR | 请求参数验证失败 |
| 401 | UNAUTHORIZED | 未认证或 Token 过期 |
| 403 | FORBIDDEN | 权限不足 |
| 404 | RESOURCE_NOT_FOUND | 资源不存在 |
| 409 | DUPLICATE_RESOURCE | 资源已存在 |
| 422 | BUSINESS_ERROR | 业务逻辑错误 |
| 500 | INTERNAL_ERROR | 服务器内部错误 |
| 503 | SERVICE_UNAVAILABLE | 数据库不可用 |
