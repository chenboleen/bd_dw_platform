# Docker 部署操作指南

## 目录
- [系统概述](#系统概述)
- [前置要求](#前置要求)
- [服务架构](#服务架构)
- [快速部署](#快速部署)
- [详细构建步骤](#详细构建步骤)
- [常用命令](#常用命令)
- [故障排查](#故障排查)
- [配置说明](#配置说明)

---

## 系统概述

本项目是一个企业级数据仓库元数据管理平台，采用 Docker Compose 进行容器化部署，包含以下核心服务：

| 服务 | 版本 | 端口 | 用途 |
|------|------|------|------|
| MySQL | 8.0 | 3306 | 关系型数据库，存储元数据 |
| Redis | 7-alpine | 6379 | 缓存服务，提升性能 |
| Elasticsearch | 8.11.0 | 9200/9300 | 全文搜索引擎 |
| Backend | Spring Boot 3.2 | 8080 | Java后端API服务 |
| Frontend | Vue 3 + Nginx | 80 | 前端Web界面 |

---

## 前置要求

### 硬件要求
- CPU: 2核及以上
- 内存: 4GB及以上（推荐8GB）
- 磁盘: 20GB及以上可用空间

### 软件要求
- Docker: 20.10+
- Docker Compose: 2.0+
- Node.js: 18+（仅本地构建前端需要）
- npm: 9+（仅本地构建前端需要）

### 验证环境
```bash
# 检查 Docker 版本
docker --version

# 检查 Docker Compose 版本
docker-compose --version

# 检查 Node.js 版本（如需要本地构建前端）
node --version
npm --version
```

---

## 服务架构

### 网络架构
```
┌─────────────────────────────────────────────────────────┐
│                     metadata-network                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │  MySQL   │  │  Redis   │  │  Elasticsearch       │  │
│  │  :3306   │  │  :6379   │  │  :9200, :9300       │  │
│  └────┬─────┘  └────┬─────┘  └──────────┬───────────┘  │
│       │             │                     │               │
│       └─────────────┼─────────────────────┘               │
│                     │                                     │
│              ┌──────▼──────┐                              │
│              │   Backend   │                              │
│              │   :8080     │                              │
│              └──────┬──────┘                              │
│                     │                                     │
│              ┌──────▼──────┐                              │
│              │  Frontend   │                              │
│              │    :80      │                              │
│              └─────────────┘                              │
└─────────────────────────────────────────────────────────┘
```

### 数据卷配置
| 卷名 | 挂载路径 | 用途 |
|------|----------|------|
| metadata-mysql-data | /var/lib/mysql | MySQL数据持久化 |
| metadata-redis-data | /data | Redis数据持久化 |
| metadata-elasticsearch-data | /usr/share/elasticsearch/data | ES数据持久化 |
| metadata-backend-exports | /app/exports | 后端导出文件 |
| metadata-backend-logs | /app/logs | 后端日志文件 |

---

## 快速部署

### 方式一：一键部署（推荐）

```bash
# 1. 进入项目根目录
cd kiro_bd_dw

# 2. 复制环境变量配置文件
cp .env.example .env

# 3. （可选）根据需要修改 .env 配置
# 编辑 .env 文件，修改数据库密码、JWT密钥等

# 4. 构建并启动所有服务
docker-compose up -d --build

# 5. 等待服务启动完成（约2-5分钟）
# 查看服务状态
docker-compose ps

# 6. 查看服务日志
docker-compose logs -f
```

### 方式二：分步部署

```bash
# 1. 构建基础服务（MySQL、Redis、Elasticsearch）
docker-compose up -d mysql redis elasticsearch

# 2. 等待基础服务健康
docker-compose ps

# 3. 构建并启动后端
docker-compose up -d --build backend

# 4. 本地构建前端（重要！）
cd frontend
npm install
npm run build
cd ..

# 5. 构建并启动前端
docker-compose up -d --build frontend

# 6. 查看所有服务状态
docker-compose ps
```

---

## 详细构建步骤

### 步骤 1：环境准备

#### 1.1 克隆或获取项目
```bash
# 如果从Git克隆
git clone <repository-url>
cd kiro_bd_dw
```

#### 1.2 配置环境变量
```bash
# 复制示例配置
cp .env.example .env

# 编辑 .env 文件，主要配置项：
# - MYSQL_ROOT_PASSWORD: MySQL root密码
# - MYSQL_PASSWORD: 业务数据库密码
# - JWT_SECRET: JWT签名密钥（生产环境必须修改）
```

#### 1.3 检查端口占用
```bash
# Windows
netstat -ano | findstr ":3306 :6379 :8080 :9200 :80"

# Linux/Mac
netstat -tulpn | grep -E ":(3306|6379|8080|9200|80)"
```

如果端口被占用，修改 `docker-compose.yml` 中的端口映射。

---

### 步骤 2：MySQL 数据库部署

#### 2.1 MySQL 配置要点

**配置文件位置**: `docker-compose.yml:12-45`

**关键配置**:
- 字符集: utf8mb4（支持中文和emoji）
- 时区: Asia/Shanghai
- 初始化脚本: `schema.sql` 自动执行
- 数据持久化: 使用命名卷 `mysql_data`

**健康检查**:
```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD:-root123456}"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 30s
```

#### 2.2 MySQL 相关命令

```bash
# 启动 MySQL
docker-compose up -d mysql

# 查看 MySQL 日志
docker-compose logs -f mysql

# 进入 MySQL 容器
docker-compose exec mysql bash

# 连接 MySQL（容器内）
mysql -u root -p
# 或使用业务用户
mysql -u metadata_user -p metadata_db

# 从宿主机连接 MySQL
mysql -h 127.0.0.1 -P 3306 -u root -p

# 备份数据库
docker-compose exec mysql mysqldump -u root -p metadata_db > backup.sql

# 恢复数据库
docker-compose exec -T mysql mysql -u root -p metadata_db < backup.sql

# 查看 MySQL 字符集
docker-compose exec mysql mysql -u root -p -e "SHOW VARIABLES LIKE 'character%';"
```

#### 2.3 MySQL 数据初始化

项目会自动执行初始化：
1. 创建数据库: `metadata_db`
2. 创建用户: `metadata_user`
3. 执行 schema: `backend-java/src/main/resources/schema.sql`
4. （可选）执行测试数据: 需要手动导入 `test-data.sql`

**手动导入测试数据**:
```bash
# 方式1：使用 docker exec
docker-compose exec -T mysql mysql -u root -proot123456 metadata_db < backend-java/src/main/resources/test-data.sql

# 方式2：进入容器后执行
docker-compose exec mysql bash
mysql -u root -p metadata_db
source /docker-entrypoint-initdb.d/test-data.sql  # 如果已挂载
```

---

### 步骤 3：Redis 缓存部署

#### 3.1 Redis 配置要点

**配置文件位置**: `docker-compose.yml:50-67`

**关键配置**:
- 镜像: redis:7-alpine（轻量级）
- 配置文件: `config/redis.conf`
- 数据持久化: RDB + AOF
- 无密码认证（开发环境）

**健康检查**:
```yaml
healthcheck:
  test: ["CMD", "redis-cli", "ping"]
  interval: 30s
  timeout: 5s
  retries: 3
  start_period: 10s
```

#### 3.2 Redis 相关命令

```bash
# 启动 Redis
docker-compose up -d redis

# 查看 Redis 日志
docker-compose logs -f redis

# 进入 Redis 容器
docker-compose exec redis sh

# 连接 Redis CLI
docker-compose exec redis redis-cli

# 测试 Redis
docker-compose exec redis redis-cli ping
# 应该返回: PONG

# 查看 Redis 信息
docker-compose exec redis redis-cli info

# 清空 Redis 数据
docker-compose exec redis redis-cli FLUSHALL

# 查看 Redis 键
docker-compose exec redis redis-cli KEYS *

# 从宿主机连接 Redis
redis-cli -h 127.0.0.1 -p 6379
```

---

### 步骤 4：Elasticsearch 搜索引擎部署

#### 4.1 Elasticsearch 配置要点

**配置文件位置**: `docker-compose.yml:72-101`

**关键配置**:
- 单节点模式: `discovery.type=single-node`
- 安全禁用: `xpack.security.enabled=false`（开发环境）
- SSL禁用: `xpack.security.http.ssl.enabled=false`
- 内存限制: 512MB（可根据需要调整）
- 系统限制: `memlock` 和 `nofile` 需调整

**重要**: Elasticsearch 需要修改系统限制（Linux）:
```bash
# 临时修改（重启失效）
sudo sysctl -w vm.max_map_count=262144

# 永久修改
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

Windows/WSL2 用户通常不需要此步骤。

**健康检查**:
```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 60s
```

#### 4.2 Elasticsearch 相关命令

```bash
# 启动 Elasticsearch
docker-compose up -d elasticsearch

# 查看 Elasticsearch 日志
docker-compose logs -f elasticsearch

# 进入 Elasticsearch 容器
docker-compose exec elasticsearch bash

# 检查集群健康状态
curl -X GET "http://localhost:9200/_cluster/health?pretty"

# 查看节点信息
curl -X GET "http://localhost:9200/_nodes?pretty"

# 查看所有索引
curl -X GET "http://localhost:9200/_cat/indices?v"

# 搜索测试
curl -X GET "http://localhost:9200/_search?pretty"

# 删除所有索引（谨慎使用！）
curl -X DELETE "http://localhost:9200/*"

# 查看索引映射
curl -X GET "http://localhost:9200/metadata_tables/_mapping?pretty"
```

---

### 步骤 5：后端服务部署

#### 5.1 后端配置要点

**Dockerfile 位置**: `backend-java/Dockerfile`

**构建策略**: 多阶段构建
- 阶段1（Builder）: Maven 构建 JAR
- 阶段2（Runtime）: JRE 运行应用

**关键优化**:
- 层缓存: 先复制 pom.xml，下载依赖后再复制源码
- 非 root 用户: 安全最佳实践
- JVM 参数优化: G1 垃圾回收，内存限制
- 健康检查: Actuator 端点

**配置文件位置**: `docker-compose.yml:106-142`

**环境变量**:
- `SPRING_PROFILES_ACTIVE`: prod
- `SPRING_DATASOURCE_URL`: MySQL 连接（使用服务名 mysql）
- `SPRING_DATA_REDIS_HOST`: Redis 连接（使用服务名 redis）
- `ELASTICSEARCH_URIS`: ES 连接（使用服务名 elasticsearch）
- `JAVA_OPTS`: JVM 参数

**健康检查**:
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
  interval: 30s
  timeout: 10s
  retries: 5
  start_period: 90s
```

#### 5.2 后端相关命令

```bash
# 构建并启动后端
docker-compose up -d --build backend

# 仅构建不启动
docker-compose build backend

# 查看后端日志
docker-compose logs -f backend

# 查看最后 100 行日志
docker-compose logs --tail=100 backend

# 进入后端容器
docker-compose exec backend sh

# 检查应用健康状态
curl http://localhost:8080/actuator/health

# 查看应用信息
curl http://localhost:8080/actuator/info

# 访问 Swagger UI
# 浏览器打开: http://localhost:8080/swagger-ui.html

# 查看导出文件
docker-compose exec backend ls -la /app/exports

# 查看日志文件
docker-compose exec backend ls -la /app/logs

# 重启后端（不重新构建）
docker-compose restart backend

# 停止后端
docker-compose stop backend

# 删除后端容器
docker-compose down backend
```

#### 5.3 后端构建缓存优化

Dockerfile 使用了层缓存策略，修改代码不会重新下载依赖：

```dockerfile
# 先复制 pom.xml，这层会被缓存
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再复制源码，只有代码变更时才重新构建
COPY src ./src
RUN mvn clean package -DskipTests -B
```

如果 pom.xml 变更，会重新下载依赖。

---

### 步骤 6：前端服务部署

#### 6.1 前端配置要点

**重要**: 前端采用**本地预构建**方案，原因：
- 避免 Docker Hub node 镜像访问问题
- 加快构建速度
- 便于本地调试

**Dockerfile 位置**: `frontend/Dockerfile-local`

**构建流程**:
1. 本地执行 `npm run build` 生成 `dist` 目录
2. Docker 仅将 `dist` 复制到 Nginx 容器
3. Nginx 提供静态文件服务

**Nginx 配置**: `frontend/nginx.conf`
- API 代理: `/api` → `http://backend:8080`
- SPA 路由支持: `try_files $uri $uri/ /index.html`
- Gzip 压缩: 开启
- 缓存策略: 静态资源长期缓存

**配置文件位置**: `docker-compose.yml:147-165`

**健康检查**:
```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:80/"]
  interval: 30s
  timeout: 5s
  retries: 3
  start_period: 10s
```

#### 6.2 前端相关命令

```bash
# ==================== 本地构建 ====================

# 进入前端目录
cd frontend

# 安装依赖（首次或依赖变更时）
npm install

# 开发模式运行（本地调试）
npm run dev
# 访问: http://localhost:3000

# 生产构建（必须执行！）
npm run build
# 生成 dist 目录

# 查看构建产物
ls -la dist

# 返回项目根目录
cd ..

# ==================== Docker 部署 ====================

# 构建并启动前端
docker-compose up -d --build frontend

# 查看前端日志
docker-compose logs -f frontend

# 进入前端容器
docker-compose exec frontend sh

# 测试前端服务
curl -I http://localhost:80

# 重启前端
docker-compose restart frontend

# ==================== 访问 ====================

# 浏览器打开前端
# http://localhost

# 测试 API 代理
curl http://localhost/api/v1/actuator/health
```

#### 6.3 前端构建常见问题

**问题1**: dist 目录不存在
```bash
# 解决方案: 确保在 frontend 目录执行构建
cd frontend
npm install
npm run build
ls -la dist  # 检查是否生成
```

**问题2**: Nginx 403 Forbidden
```bash
# 检查文件权限
docker-compose exec frontend ls -la /usr/share/nginx/html

# 解决方案: 重新构建
docker-compose up -d --build frontend
```

**问题3**: 前端页面空白，API 报错
```bash
# 检查后端是否健康
docker-compose ps

# 检查 Nginx 配置
docker-compose exec frontend cat /etc/nginx/conf.d/default.conf
```

---

## 常用命令

### Docker Compose 常用命令

```bash
# ==================== 生命周期管理 ====================

# 启动所有服务
docker-compose up -d

# 构建并启动所有服务
docker-compose up -d --build

# 启动指定服务
docker-compose up -d mysql redis

# 停止所有服务
docker-compose stop

# 停止指定服务
docker-compose stop backend

# 重启所有服务
docker-compose restart

# 重启指定服务
docker-compose restart frontend

# 删除所有容器（保留数据卷）
docker-compose down

# 删除所有容器和数据卷（谨慎！）
docker-compose down -v

# ==================== 状态查看 ====================

# 查看服务状态
docker-compose ps

# 查看所有服务日志
docker-compose logs

# 查看所有服务日志（实时跟踪）
docker-compose logs -f

# 查看指定服务日志
docker-compose logs backend

# 查看指定服务日志（实时跟踪）
docker-compose logs -f frontend

# 查看最后 N 行日志
docker-compose logs --tail=100 backend

# ==================== 构建管理 ====================

# 构建所有服务
docker-compose build

# 构建指定服务
docker-compose build backend

# 无缓存构建
docker-compose build --no-cache frontend

# ==================== 进入容器 ====================

# 进入 MySQL
docker-compose exec mysql bash

# 进入 Redis
docker-compose exec redis sh

# 进入 Elasticsearch
docker-compose exec elasticsearch bash

# 进入后端
docker-compose exec backend sh

# 进入前端
docker-compose exec frontend sh

# ==================== 资源管理 ====================

# 查看资源使用
docker-compose stats

# 清理未使用的镜像
docker image prune

# 清理未使用的资源
docker system prune
```

### 各服务独立命令

#### MySQL
```bash
# 连接
docker-compose exec mysql mysql -u root -p

# 备份
docker-compose exec mysql mysqldump -u root -p metadata_db > backup_$(date +%Y%m%d).sql

# 恢复
docker-compose exec -T mysql mysql -u root -p metadata_db < backup.sql

# 查看慢查询日志
docker-compose exec mysql tail -f /var/log/mysql/slow.log
```

#### Redis
```bash
# 连接 CLI
docker-compose exec redis redis-cli

# 查看所有键
docker-compose exec redis redis-cli KEYS "*"

# 查看内存使用
docker-compose exec redis redis-cli INFO memory

# 导出数据
docker-compose exec redis redis-cli BGSAVE
```

#### Elasticsearch
```bash
# 健康检查
curl http://localhost:9200/_cluster/health?pretty

# 查看索引
curl http://localhost:9200/_cat/indices?v

# 重新索引
curl -X POST "http://localhost:9200/_reindex?pretty" -H 'Content-Type: application/json' -d'
{
  "source": {"index": "old_index"},
  "dest": {"index": "new_index"}
}'
```

#### 后端
```bash
# 健康检查
curl http://localhost:8080/actuator/health

# 查看环境变量
docker-compose exec backend env

# 线程 dump
docker-compose exec backend jstack 1 > thread_dump.txt

# Heap dump
docker-compose exec backend jmap -dump:format=b,file=/app/logs/heap.hprof 1
```

#### 前端/Nginx
```bash
# 测试 Nginx 配置
docker-compose exec frontend nginx -t

# 重载 Nginx 配置
docker-compose exec frontend nginx -s reload

# 查看 Nginx 访问日志
docker-compose exec frontend tail -f /var/log/nginx/access.log

# 查看 Nginx 错误日志
docker-compose exec frontend tail -f /var/log/nginx/error.log
```

---

## 故障排查

### 服务启动顺序问题

**症状**: 后端启动失败，数据库连接错误

**原因**: 后端依赖 MySQL/Redis/ES，但这些服务尚未完全启动

**解决方案**:
```bash
# 1. 先启动基础服务
docker-compose up -d mysql redis elasticsearch

# 2. 等待基础服务健康
docker-compose ps
# 确保 status 显示 healthy

# 3. 再启动后端和前端
docker-compose up -d backend frontend
```

### MySQL 相关问题

#### 问题 1: 字符集乱码
**症状**: 中文字符显示为 ???

**解决方案**:
```bash
# 检查 MySQL 字符集
docker-compose exec mysql mysql -u root -p -e "SHOW VARIABLES LIKE 'character%';"

# 确保返回:
# character_set_server = utf8mb4
# character_set_database = utf8mb4

# 修改表字符集（如果已创建）
docker-compose exec mysql mysql -u root -p metadata_db
ALTER TABLE table_metadata CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE column_metadata CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 问题 2: 数据卷权限问题
**症状**: MySQL 无法写入数据

**解决方案**:
```bash
# 查看卷挂载
docker volume inspect metadata-mysql-data

# 删除卷重新创建（会丢失数据！）
docker-compose down -v
docker-compose up -d mysql
```

#### 问题 3: 连接被拒绝
**症状**: `Communications link failure`

**检查**:
```bash
# 查看 MySQL 状态
docker-compose ps

# 查看 MySQL 日志
docker-compose logs mysql

# 测试端口连通性
telnet localhost 3306
# 或
nc -zv localhost 3306
```

### Redis 相关问题

#### 问题 1: 内存不足
**症状**: Redis OOM 被 Kill

**解决方案**:
```bash
# 检查 Redis 内存使用
docker-compose exec redis redis-cli INFO memory

# 修改 redis.conf，增加 maxmemory
# config/redis.conf
maxmemory 512mb
maxmemory-policy allkeys-lru

# 重启 Redis
docker-compose restart redis
```

#### 问题 2: 持久化失败
**症状**: AOF/RDB 写入错误

**解决方案**:
```bash
# 检查数据卷权限
docker volume inspect metadata-redis-data

# 手动触发保存
docker-compose exec redis redis-cli BGSAVE

# 查看日志
docker-compose logs redis
```

### Elasticsearch 相关问题

#### 问题 1: vm.max_map_count 太小
**症状**: ES 启动失败，提示 max virtual memory areas too low

**解决方案** (Linux):
```bash
# 临时
sudo sysctl -w vm.max_map_count=262144

# 永久
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p
```

#### 问题 2: 磁盘空间不足
**症状**: ES 进入只读模式

**解决方案**:
```bash
# 检查磁盘
docker system df

# 清理 ES 索引
curl -X DELETE "http://localhost:9200/old_indices_*"

# 或者清理 Docker
docker system prune -a
```

#### 问题 3: 健康检查失败
**症状**: ES 容器一直 restarting

**解决方案**:
```bash
# 查看 ES 日志
docker-compose logs elasticsearch

# 常见原因:
# 1. 内存不足 → 调整 ES_JAVA_OPTS
# 2. 权限问题 → 检查数据卷
# 3. 端口冲突 → 修改端口映射
```

### 后端相关问题

#### 问题 1: 启动超时
**症状**: 后端一直 unhealthy

**解决方案**:
```bash
# 查看后端日志
docker-compose logs backend

# 增加健康检查等待时间
# docker-compose.yml
healthcheck:
  start_period: 120s  # 从 90s 增加到 120s

# 或者先不检查健康，直接进入容器调试
docker-compose run --rm backend sh
java -jar app.jar
```

#### 问题 2: 数据库连接失败
**症状**: `Communications link failure`

**检查清单**:
1. MySQL 容器是否运行: `docker-compose ps`
2. MySQL 是否 healthy
3. 环境变量配置正确: `SPRING_DATASOURCE_URL`
4. 网络连通: `docker-compose exec backend ping mysql`

```bash
# 从后端容器测试 MySQL
docker-compose exec backend sh
curl -v telnet://mysql:3306
```

#### 问题 3: OOM 被 Kill
**症状**: 后端容器被 OOM Killed

**解决方案**:
```bash
# 调整 JVM 内存
# docker-compose.yml
environment:
  JAVA_OPTS: "-Xms256m -Xmx512m ..."  # 增加 -Xmx

# 或者增加 Docker 内存限制
# docker-compose.yml
deploy:
  resources:
    limits:
      memory: 1G
```

### 前端相关问题

#### 问题 1: dist 目录缺失
**症状**: 前端构建失败，提示 no such file or directory

**解决方案**:
```bash
# 确保在 frontend 目录执行构建
cd frontend
npm install
npm run build
ls -la dist  # 确认生成

# 返回根目录再部署
cd ..
docker-compose up -d --build frontend
```

#### 问题 2: API 请求 404/502
**症状**: 前端页面可以打开，但 API 调用失败

**检查**:
1. 后端是否健康: `docker-compose ps backend`
2. Nginx 配置正确:
```bash
docker-compose exec frontend cat /etc/nginx/conf.d/default.conf
```
3. 测试代理:
```bash
docker-compose exec frontend curl http://backend:8080/actuator/health
```

#### 问题 3: 静态资源 404
**症状**: CSS/JS 文件加载失败

**解决方案**:
```bash
# 检查 dist 目录文件
ls -la frontend/dist

# 重新构建前端
cd frontend
npm run build
cd ..

# 重新构建镜像
docker-compose up -d --build frontend
```

### 网络相关问题

#### 问题: 容器间无法通信
**症状**: backend 无法连接 mysql

**解决方案**:
```bash
# 检查网络
docker network ls
docker network inspect metadata-network

# 确保所有容器在同一网络
docker-compose ps

# 手动测试连通性
docker-compose exec backend ping mysql
docker-compose exec backend ping redis
docker-compose exec backend ping elasticsearch
```

---

## 配置说明

### 环境变量配置 (.env)

复制 `.env.example` 为 `.env`，根据需要修改：

```bash
# ==================== MySQL 配置 ====================
MYSQL_ROOT_PASSWORD=root123456          # MySQL root 密码
MYSQL_DATABASE=metadata_db                 # 数据库名
MYSQL_USER=metadata_user                   # 业务用户名
MYSQL_PASSWORD=metadata_pass               # 业务用户密码
MYSQL_PORT=3306                            # MySQL 端口

# ==================== Redis 配置 ====================
REDIS_PORT=6379                            # Redis 端口

# ==================== Elasticsearch 配置 ====================
ES_PORT=9200                               # ES HTTP 端口

# ==================== 后端配置 ====================
BACKEND_PORT=8080                          # 后端端口
JWT_SECRET=kiro-metadata-dw-2024-secure-jwt-signing-key-base64-encoded-minimum-256-bits

# ==================== 前端配置 ====================
FRONTEND_PORT=80                            # 前端端口
```

**生产环境注意事项**:
1. 必须修改 `MYSQL_ROOT_PASSWORD` 和 `MYSQL_PASSWORD`
2. 必须修改 `JWT_SECRET` 为强密码
3. 考虑开启 MySQL/Redis 密码认证
4. 考虑开启 Elasticsearch 安全配置

### docker-compose.yml 配置详解

#### MySQL 服务配置
```yaml
mysql:
  image: mysql:8.0
  container_name: metadata-mysql
  environment:
    MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123456}  # 默认值
    MYSQL_DATABASE: ${MYSQL_DATABASE:-metadata_db}
    TZ: Asia/Shanghai                  # 时区
    LANG: C.UTF-8                      # 字符集环境变量
    LC_ALL: C.UTF-8
  command:
    - --character-set-server=utf8mb4   # 服务器字符集
    - --collation-server=utf8mb4_unicode_ci
  volumes:
    - mysql_data:/var/lib/mysql        # 数据持久化
    - ./backend-java/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro
```

#### Redis 服务配置
```yaml
redis:
  image: redis:7-alpine
  volumes:
    - redis_data:/data                  # 数据持久化
    - ./config/redis.conf:/etc/redis/redis.conf:ro
  command: redis-server /etc/redis/redis.conf
```

#### Elasticsearch 服务配置
```yaml
elasticsearch:
  image: elasticsearch:8.11.0
  environment:
    - discovery.type=single-node        # 单节点模式
    - xpack.security.enabled=false       # 禁用安全（开发环境）
    - ES_JAVA_OPTS=-Xms512m -Xmx512m   # 内存限制
  ulimits:
    memlock:
      soft: -1
      hard: -1
    nofile:
      soft: 65536
      hard: 65536
```

#### 后端服务配置
```yaml
backend:
  build:
    context: ./backend-java
    dockerfile: Dockerfile
  environment:
    SPRING_PROFILES_ACTIVE: prod
    SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE:-metadata_db}?...
    # 注意: 使用服务名 mysql, redis, elasticsearch 作为主机名
  depends_on:
    mysql:
      condition: service_healthy        # 等待 MySQL 健康
    redis:
      condition: service_healthy
    elasticsearch:
      condition: service_healthy
```

#### 前端服务配置
```yaml
frontend:
  build:
    context: ./frontend
    dockerfile: Dockerfile-local        # 使用本地预构建
  depends_on:
    backend:
      condition: service_healthy
```

### Nginx 配置 (frontend/nginx.conf)

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    # Gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;

    # API 代理到后端
    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # SPA 路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

---

## 附录

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 开发者 | developer | dev123 |
| 访客 | guest | guest123 |

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端界面 | http://localhost |
| 后端 API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Elasticsearch | http://localhost:9200 |

### 端口列表

| 服务 | 宿主端口 | 容器端口 | 协议 |
|------|----------|----------|------|
| MySQL | 3306 | 3306 | TCP |
| Redis | 6379 | 6379 | TCP |
| Elasticsearch | 9200 | 9200 | HTTP |
| Elasticsearch | 9300 | 9300 | TCP |
| Backend | 8080 | 8080 | HTTP |
| Frontend | 80 | 80 | HTTP |

### 数据备份建议

#### 定期备份脚本示例 (backup.sh)

```bash
#!/bin/bash
BACKUP_DIR="./backups"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# 备份 MySQL
docker-compose exec mysql mysqldump -u root -p'root123456' metadata_db | gzip > $BACKUP_DIR/mysql_$DATE.sql.gz

# 备份 Redis
docker-compose exec redis redis-cli BGSAVE
sleep 5
docker cp $(docker-compose ps -q redis):/data/dump.rdb $BACKUP_DIR/redis_$DATE.rdb

# 备份 Elasticsearch（使用 snapshot 更好）
curl -X PUT "http://localhost:9200/_snapshot/backup/snapshot_$DATE?wait_for_completion=true"

# 保留最近 7 天的备份
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
find $BACKUP_DIR -name "*.rdb" -mtime +7 -delete

echo "Backup completed: $DATE"
```

### 生产环境部署建议

1. **安全加固**
   - 修改所有默认密码
   - 开启 MySQL/Redis 认证
   - 开启 Elasticsearch Security
   - 使用 HTTPS (Let's Encrypt)
   - 配置防火墙规则

2. **高可用**
   - MySQL 主从复制
   - Redis 哨兵/集群
   - Elasticsearch 集群
   - 后端多实例 + 负载均衡

3. **监控告警**
   - Prometheus + Grafana
   - ELK Stack 日志收集
   - 健康检查告警
   - 资源使用监控

4. **数据管理**
   - 定期自动备份
   - 异地备份
   - 灾难恢复演练
   - 数据加密

---

## 联系与支持

如遇问题，请查看：
- 项目 README.md
- 日志文件: `docker-compose logs`
- 各服务官方文档

---

**文档版本**: 1.0.0  
**最后更新**: 2026-03-15  
**维护者**: Kiro Team
