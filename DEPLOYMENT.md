# 部署指南

## 环境要求

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Docker | 20.10 | 24.x |
| Docker Compose | 2.0 | 2.20+ |
| 内存 | 4GB | 8GB+ |
| 磁盘 | 20GB | 50GB+ |
| CPU | 2核 | 4核+ |

## 生产环境部署

### 1. 准备环境变量

```bash
cp .env.example .env
```

编辑 `.env`，**必须修改以下配置**：

```env
# 数据库密码（使用强密码）
MYSQL_ROOT_PASSWORD=<强密码>
MYSQL_PASSWORD=<强密码>

# JWT 密钥（至少 256 位随机字符串）
JWT_SECRET=<随机字符串>
```

生成强随机密钥：
```bash
openssl rand -base64 64
```

### 2. 构建并启动服务

```bash
# 构建镜像并启动（首次部署）
docker-compose up -d --build

# 后续更新（重新构建）
docker-compose up -d --build backend frontend
```

### 3. 验证部署

```bash
# 检查所有服务状态
docker-compose ps

# 检查后端健康状态
curl http://localhost:8080/actuator/health

# 检查前端
curl http://localhost:80
```

### 4. 初始化数据库

数据库 schema 会在 MySQL 容器首次启动时自动执行 `schema.sql`。

如需手动初始化：
```bash
docker-compose exec mysql mysql -u root -p metadata_db < backend-java/src/main/resources/schema.sql
```

## 服务管理

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（危险：会删除所有数据）
docker-compose down -v

# 重启单个服务
docker-compose restart backend

# 查看日志
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs --tail=100 mysql

# 进入容器
docker-compose exec backend sh
docker-compose exec mysql mysql -u root -p
```

## 数据备份

### MySQL 备份

```bash
# 备份数据库
docker-compose exec mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} metadata_db > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker-compose exec -T mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} metadata_db < backup_20240101.sql
```

### Redis 备份

```bash
# 触发 RDB 快照
docker-compose exec redis redis-cli BGSAVE

# 复制 RDB 文件
docker cp metadata-redis:/data/dump.rdb ./redis_backup_$(date +%Y%m%d).rdb
```

### Elasticsearch 备份

```bash
# 创建快照仓库（需先配置）
curl -X PUT "localhost:9200/_snapshot/backup_repo" -H 'Content-Type: application/json' -d'
{
  "type": "fs",
  "settings": {
    "location": "/usr/share/elasticsearch/backup"
  }
}'

# 创建快照
curl -X PUT "localhost:9200/_snapshot/backup_repo/snapshot_$(date +%Y%m%d)"
```

## 故障排查

### 后端启动失败

```bash
# 查看详细日志
docker-compose logs backend

# 常见原因：
# 1. 数据库未就绪 → 等待 MySQL 健康检查通过
# 2. 端口冲突 → 修改 .env 中的端口配置
# 3. 内存不足 → 调整 JAVA_OPTS 中的 -Xmx 参数
```

### 数据库连接失败

```bash
# 检查 MySQL 状态
docker-compose exec mysql mysqladmin -u root -p ping

# 检查网络连通性
docker-compose exec backend ping mysql
```

### Elasticsearch 无法启动

```bash
# 检查系统 vm.max_map_count（Linux）
sysctl vm.max_map_count
# 如果小于 262144，执行：
sudo sysctl -w vm.max_map_count=262144
# 永久生效：
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```

## 性能调优

### JVM 参数调整

在 `docker-compose.yml` 中修改 `JAVA_OPTS`：

```yaml
environment:
  JAVA_OPTS: "-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### MySQL 参数调整

在 `docker-compose.yml` 的 MySQL command 中调整：

```yaml
command:
  - --innodb-buffer-pool-size=2G  # 建议为可用内存的 70%
  - --max-connections=500
```

### Elasticsearch 内存调整

```yaml
environment:
  - ES_JAVA_OPTS=-Xms1g -Xmx1g  # 建议不超过系统内存的 50%
```
