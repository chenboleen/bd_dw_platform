# 开发指南

## 开发环境搭建

### 前置要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | 后端运行环境 |
| Maven | 3.8+ | 后端构建工具 |
| Node.js | 18+ | 前端运行环境 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 7.x | 缓存服务 |
| Elasticsearch | 8.x | 搜索引擎 |

### 本地开发启动

#### 方式一：使用 Docker Compose（推荐）

```bash
# 仅启动基础设施（数据库、缓存、搜索）
docker-compose up -d mysql redis elasticsearch

# 本地启动后端
cd backend-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 本地启动前端
cd frontend
npm install
npm run dev
```

#### 方式二：完全本地

1. 安装并启动 MySQL 8.0，创建数据库：
```sql
CREATE DATABASE metadata_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 安装并启动 Redis 7.x

3. 安装并启动 Elasticsearch 8.x（关闭安全认证）

4. 启动后端：
```bash
cd backend-java
mvn spring-boot:run
```

5. 启动前端：
```bash
cd frontend
npm install
npm run dev
```

## 项目结构

```
kiro_bd_dw/
├── backend-java/                    # Spring Boot 后端
│   ├── src/main/java/com/kiro/metadata/
│   │   ├── annotation/              # 自定义注解
│   │   ├── aspect/                  # AOP 切面
│   │   ├── config/                  # 配置类
│   │   ├── controller/              # API 控制器
│   │   ├── document/                # Elasticsearch 文档
│   │   ├── dto/                     # 数据传输对象
│   │   │   ├── request/             # 请求 DTO
│   │   │   └── response/            # 响应 DTO
│   │   ├── entity/                  # JPA 实体类
│   │   ├── exception/               # 自定义异常
│   │   ├── handler/                 # 处理器
│   │   ├── interceptor/             # 拦截器
│   │   ├── repository/              # 数据访问层
│   │   ├── security/                # 安全相关
│   │   ├── service/                 # 业务逻辑层
│   │   └── util/                    # 工具类
│   └── src/main/resources/
│       ├── application.yml          # 主配置
│       ├── application-dev.yml      # 开发环境配置
│       ├── application-prod.yml     # 生产环境配置
│       └── schema.sql               # 数据库初始化脚本
├── frontend/                        # Vue.js 前端
│   ├── src/
│   │   ├── api/                     # API 调用封装
│   │   ├── components/              # 通用组件
│   │   ├── router/                  # 路由配置
│   │   ├── stores/                  # Pinia 状态管理
│   │   ├── types/                   # TypeScript 类型定义
│   │   └── views/                   # 页面组件
│   ├── Dockerfile
│   └── nginx.conf
├── config/                          # 基础设施配置
│   └── redis.conf
├── docker-compose.yml
├── .env.example
├── README.md
├── DEPLOYMENT.md
├── API.md
├── DEVELOPMENT.md
└── USER_MANUAL.md
```

## 开发规范

### 后端规范

1. **代码注释**：所有注释、日志消息、异常信息使用中文
2. **包命名**：`com.kiro.metadata.<模块名>`
3. **异常处理**：业务异常继承 `BusinessException`，通过 `GlobalExceptionHandler` 统一处理
4. **API 响应**：统一使用 `ResponseEntity` 包装，错误使用 `ErrorResponse`
5. **日志**：使用 `@Slf4j` + `log.info/warn/error`，不使用 `System.out.println`
6. **缓存**：使用 `@Cacheable/@CacheEvict/@CachePut` 注解，缓存键格式：`{模块}:{id}`

### 前端规范

1. **组件命名**：PascalCase（如 `TableList.vue`）
2. **API 调用**：统一在 `src/api/` 目录封装，不在组件中直接调用 axios
3. **状态管理**：跨组件共享状态使用 Pinia store
4. **类型安全**：所有数据结构定义 TypeScript 接口，避免使用 `any`
5. **错误处理**：API 错误统一在 `client.ts` 拦截器处理，显示 Element Plus 消息提示

## 测试

### 后端测试

```bash
# 运行所有测试
cd backend-java
mvn test

# 运行属性测试
mvn test -Dgroups="property"

# 生成覆盖率报告
mvn jacoco:report
# 报告位置：target/site/jacoco/index.html
```

### 前端测试

```bash
cd frontend
# 类型检查
npm run build
```

## 常用命令

```bash
# 后端：清理并重新构建
cd backend-java && mvn clean package -DskipTests

# 前端：安装依赖
cd frontend && npm install

# 前端：构建生产版本
cd frontend && npm run build

# Docker：重新构建并启动
docker-compose up -d --build

# Docker：查看所有容器日志
docker-compose logs -f

# Docker：清理所有容器和镜像
docker-compose down --rmi all
```
