# Elasticsearch 快速启动指南

## 说明

bd_dw_platform 使用 `co.elastic.clients:elasticsearch-java:8.11.0` 原生 Java 客户端与 Elasticsearch 交互，
不依赖 Spring Data Elasticsearch，避免了 Spring Boot 3.2.x 的兼容性问题。

## 使用 Docker Compose 启动

### 1. 启动所有服务（包含 Elasticsearch）

```bash
docker-compose up -d
```

Elasticsearch 容器名：`bd-dw-elasticsearch`

### 2. 验证 Elasticsearch 是否启动成功

```bash
curl http://localhost:9200
```

预期输出：
```json
{
  "name" : "...",
  "cluster_name" : "docker-cluster",
  "version" : {
    "number" : "8.11.0",
    ...
  }
}
```

### 3. 检查集群健康状态

```bash
curl http://localhost:9200/_cluster/health?pretty
```

### 4. 停止服务

```bash
docker-compose down
```

### 5. 停止并删除数据卷

```bash
docker-compose down -v
```

## 手动启动 Elasticsearch

### Windows

1. 下载 Elasticsearch 8.11.0
2. 解压到本地目录
3. 编辑 `config/elasticsearch.yml`，添加：
   ```yaml
   xpack.security.enabled: false
   ```
4. 运行 `bin\elasticsearch.bat`

### Linux/Mac

1. 下载 Elasticsearch 8.11.0
2. 解压到本地目录
3. 编辑 `config/elasticsearch.yml`，添加：
   ```yaml
   xpack.security.enabled: false
   ```
4. 运行 `bin/elasticsearch`

## 应用配置

确保 `application-dev.yml` 中的配置正确：

```yaml
elasticsearch:
  uris: http://localhost:9200
```

## 常见问题

### 1. 端口被占用
修改 `docker-compose.yml` 中 `ES_PORT` 或 `.env` 文件中的端口配置。

### 2. 内存不足
调整 `docker-compose.yml` 中的 `ES_JAVA_OPTS`：
```yaml
ES_JAVA_OPTS: "-Xms256m -Xmx256m"
```

### 3. 索引未自动创建
检查应用日志，确认 `ElasticsearchIndexConfig` 是否执行成功：
```bash
docker-compose logs bd-dw-backend | grep -i elasticsearch
```
