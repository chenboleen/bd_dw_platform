# bd_dw_platform - 后端服务

## 项目简介

基于 Spring Boot 3.2+ 和 Java 17+ 的企业级数据仓库元数据管理平台后端服务。

## 技术栈

- **Java**: 17+
- **Spring Boot**: 3.2.0
- **MyBatis-Plus**: 3.5.7（spring-boot3 专用 starter）
- **Spring Security**: 6.x
- **MySQL**: 8.0+
- **Redis**: 7.x
- **Elasticsearch**: 8.11.0（co.elastic.clients 原生客户端，非 Spring Data ES）
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
mysql -u root -p
CREATE DATABASE metadata_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **使用 Docker Compose 启动所有基础设施**
```bash
cd ..
docker-compose up -d mysql redis elasticsearch
```

4. **修改配置文件**
编辑 `src/main/resources/application-dev.yml`，修改数据库连接信息

5. **编译项目**
```bash
mvn clean install
```

6. **运行应用**
```bash
mvn spring-boot:run
```

7. **访问 Swagger 文档**
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
│   │   │       ├── document/                     # ES 文档 POJO
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

- 用户认证与授权 (JWT + Spring Security)
- 表元数据管理 (CRUD)
- 字段元数据管理
- 数据血缘关系分析
- 全文搜索 (Elasticsearch)
- 数据目录管理
- 数据质量监控
- 变更历史追踪
- 批量导入导出
- SQL 解析与血缘提取

## Elasticsearch 配置说明

本项目使用 `co.elastic.clients:elasticsearch-java:8.11.0` 原生客户端，
**不使用** `spring-boot-starter-data-elasticsearch`，避免 Spring Boot 3.2.x 兼容性问题。

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

详细说明请参考: [ELASTICSEARCH_CONFIG.md](ELASTICSEARCH_CONFIG.md)

## Redis 配置

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
```

## MySQL 配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/metadata_db?characterEncoding=UTF-8
    username: root
    password: root
```

> 注意：JDBC URL 中使用 `characterEncoding=UTF-8`（Java 字符集名称），不能使用 `utf8mb4`。

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

### Docker Compose 部署（推荐）

```bash
cd ..
docker-compose up -d
```

### 单独 Docker 部署

```bash
# 构建镜像
docker build -t bd-dw-backend:1.0.0 .

# 运行容器
docker run -d -p 8080:8080 bd-dw-backend:1.0.0
```

## 开发规范

- 所有代码注释、日志消息、异常信息使用中文
- 遵循阿里巴巴 Java 开发手册
- 使用 Lombok 简化代码
- 统一异常处理
- RESTful API 设计规范

## 许可证

Copyright © 2024 Kiro
