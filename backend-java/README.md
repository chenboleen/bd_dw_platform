# 数据仓库元数据管理系统 - 后端服务

## 项目简介

基于 Spring Boot 3.2+ 和 Java 17+ 的企业级数据仓库元数据管理平台后端服务。

## 技术栈

- **Java**: 17+
- **Spring Boot**: 3.2.0
- **MyBatis-Plus**: 3.5.5
- **Spring Security**: 6.x
- **MySQL**: 8.0+
- **Redis**: 7.x
- **Elasticsearch**: 8.x
- **JWT**: 0.12.3
- **Maven**: 3.8+

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.x
- Elasticsearch 8.x

### 启动步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd kiro_bd_dw/backend-java
```

2. **配置数据库**
```bash
# 创建数据库
mysql -u root -p
CREATE DATABASE metadata_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **启动 Redis**
```bash
# 使用 Docker 启动 Redis
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

4. **启动 Elasticsearch**
```bash
# 使用 Docker Compose 启动 Elasticsearch
cd ..
docker-compose -f docker-compose-elasticsearch.yml up -d

# 验证 Elasticsearch 是否启动成功
curl http://localhost:9200
```

详细说明请参考: [ELASTICSEARCH_SETUP.md](../ELASTICSEARCH_SETUP.md)

5. **修改配置文件**
编辑 `src/main/resources/application-dev.yml`,修改数据库连接信息

6. **编译项目**
```bash
mvn clean install
```

7. **运行应用**
```bash
mvn spring-boot:run
```

8. **访问 Swagger 文档**
打开浏览器访问: http://localhost:8080/swagger-ui.html

## 项目结构

```
backend-java/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/kiro/metadata/
│   │   │       ├── MetadataApplication.java      # 主应用类
│   │   │       ├── config/                       # 配置类
│   │   │       ├── controller/                   # 控制器
│   │   │       ├── service/                      # 服务层
│   │   │       ├── repository/                   # 数据访问层
│   │   │       ├── entity/                       # 实体类
│   │   │       ├── dto/                          # 数据传输对象
│   │   │       ├── security/                     # 安全相关
│   │   │       ├── exception/                    # 异常处理
│   │   │       └── util/                         # 工具类
│   │   └── resources/
│   │       ├── application.yml                   # 主配置文件
│   │       ├── application-dev.yml               # 开发环境配置
│   │       ├── application-prod.yml              # 生产环境配置
│   │       └── logback-spring.xml                # 日志配置
│   └── test/                                     # 测试代码
├── pom.xml                                       # Maven 配置
└── README.md                                     # 项目说明
```

## 核心功能

- ✅ 用户认证与授权 (JWT + Spring Security)
- ✅ 表元数据管理 (CRUD)
- ✅ 字段元数据管理
- ✅ 数据血缘关系分析
- ✅ 全文搜索 (Elasticsearch)
- ✅ 数据目录管理
- ✅ 数据质量监控
- ✅ 变更历史追踪
- ✅ 批量导入导出
- ✅ SQL 解析与血缘提取

## 配置说明

### Elasticsearch 配置

本项目使用 Elasticsearch 8.x 提供全文搜索功能。详细配置说明请参考:
- [Elasticsearch 配置文档](ELASTICSEARCH_CONFIG.md)
- [Elasticsearch 快速启动指南](../ELASTICSEARCH_SETUP.md)

**核心配置项** (`application.yml`):
```yaml
elasticsearch:
  uris: http://localhost:9200
  username:
  password:
  connection-timeout: 5s
  socket-timeout: 30s
  index:
    name: metadata_tables
    number-of-shards: 1
    number-of-replicas: 1
```

**索引结构**:
- 索引名称: `metadata_tables`
- 支持字段: 表名、描述、字段名、字段描述等
- 搜索功能: 全文搜索、精确匹配、范围查询

### Redis 配置

Redis 用于缓存和会话管理。

**配置项** (`application.yml`):
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
```

### MySQL 配置

MySQL 作为主数据库存储元数据。

**配置项** (`application-dev.yml`):
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/metadata_db
    username: root
    password: root
```

## API 文档

启动应用后访问 Swagger UI: http://localhost:8080/swagger-ui.html

## 测试

```bash
# 运行所有测试
mvn test

# 运行属性测试
mvn test -Dgroups="property"

# 生成测试覆盖率报告
mvn jacoco:report
```

## 部署

### Docker 部署

```bash
# 构建镜像
docker build -t metadata-backend:1.0.0 .

# 运行容器
docker run -d -p 8080:8080 metadata-backend:1.0.0
```

### Docker Compose 部署

```bash
cd ..
docker-compose up -d
```

## 开发规范

- 所有代码注释使用中文
- 遵循阿里巴巴 Java 开发手册
- 使用 Lombok 简化代码
- 统一异常处理
- RESTful API 设计规范

## 许可证

Copyright © 2024 Kiro
