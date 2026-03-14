# 数据仓库元数据管理系统

企业级数据仓库元数据管理平台，提供表元数据管理、血缘关系追踪、全文搜索、数据质量监控等核心功能。

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | Java 17 + Spring Boot 3.2 |
| ORM | MyBatis-Plus 3.5 + JPA |
| 认证 | Spring Security 6 + JWT |
| 缓存 | Redis 7.x |
| 搜索 | Elasticsearch 8.x |
| 数据库 | MySQL 8.0 |
| 前端框架 | Vue.js 3 + TypeScript |
| UI 组件库 | Element Plus |
| 构建工具 | Vite 5 |
| 容器化 | Docker + Docker Compose |
| Web 服务器 | Nginx |

## 功能模块

- **表元数据管理**：创建、查询、更新、删除表和字段元数据
- **血缘关系追踪**：可视化数据血缘图谱，支持影响分析
- **全文搜索**：基于 Elasticsearch 的多字段搜索和自动补全
- **数据目录**：层级目录组织，最多支持 5 级分类
- **数据质量**：质量指标记录和趋势分析
- **变更历史**：完整的元数据变更审计追踪
- **导入导出**：支持 CSV/JSON 格式批量导入导出
- **权限管理**：基于角色的访问控制（ADMIN/DEVELOPER/GUEST）

## 快速开始

### 前置要求

- Docker 20.10+
- Docker Compose 2.0+

### 一键启动

```bash
# 1. 克隆项目
git clone <repository-url>
cd kiro_bd_dw

# 2. 复制环境变量配置
cp .env.example .env
# 根据需要修改 .env 中的配置

# 3. 启动所有服务
docker-compose up -d

# 4. 查看服务状态
docker-compose ps

# 5. 查看后端日志
docker-compose logs -f backend
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端界面 | http://localhost:80 |
| 后端 API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Elasticsearch | http://localhost:9200 |

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 开发者 | developer | dev123 |
| 访客 | guest | guest123 |

## 开发指南

详见 [DEVELOPMENT.md](./DEVELOPMENT.md)

## 部署指南

详见 [DEPLOYMENT.md](./DEPLOYMENT.md)

## API 文档

详见 [API.md](./API.md) 或访问 Swagger UI

## 用户手册

详见 [USER_MANUAL.md](./USER_MANUAL.md)
