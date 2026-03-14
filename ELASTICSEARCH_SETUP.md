# Elasticsearch 快速启动指南

## 使用 Docker Compose 启动

### 1. 启动 Elasticsearch

在项目根目录执行：

```bash
docker-compose -f docker-compose-elasticsearch.yml up -d
```

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

### 4. 停止 Elasticsearch

```bash
docker-compose -f docker-compose-elasticsearch.yml down
```

### 5. 停止并删除数据卷

```bash
docker-compose -f docker-compose-elasticsearch.yml down -v
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
如果 9200 端口被占用，修改 `docker-compose-elasticsearch.yml` 中的端口映射。

### 2. 内存不足
调整 `ES_JAVA_OPTS` 中的堆内存大小：
```yaml
ES_JAVA_OPTS: "-Xms256m -Xmx256m"
```

### 3. 索引未自动创建
检查应用日志，确认 `ElasticsearchIndexConfig` 是否执行成功。
