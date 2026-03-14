# Elasticsearch 配置说明

## 概述

本项目使用 Elasticsearch 8.x 作为全文搜索引擎，为表元数据提供快速的搜索和过滤功能。

## 配置文件

### application.yml

```yaml
# Elasticsearch 配置
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

### 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `elasticsearch.uris` | Elasticsearch 服务地址，支持多个节点（逗号分隔） | http://localhost:9200 |
| `elasticsearch.username` | 认证用户名（可选） | 空 |
| `elasticsearch.password` | 认证密码（可选） | 空 |
| `elasticsearch.connection-timeout` | 连接超时时间 | 5s |
| `elasticsearch.socket-timeout` | Socket 超时时间 | 30s |
| `elasticsearch.index.name` | 索引名称 | metadata_tables |
| `elasticsearch.index.number-of-shards` | 分片数量 | 1 |
| `elasticsearch.index.number-of-replicas` | 副本数量 | 1 |

## 索引结构

### 索引名称
`metadata_tables`

### 字段映射

| 字段名 | 类型 | 说明 | 是否可搜索 |
|--------|------|------|-----------|
| `id` | keyword | 表 ID（UUID） | 否 |
| `databaseName` | keyword | 数据库名 | 精确匹配 |
| `tableName` | text + keyword | 表名 | 全文搜索 + 精确匹配 |
| `tableType` | keyword | 表类型（TABLE/VIEW/EXTERNAL） | 精确匹配 |
| `description` | text | 表描述 | 全文搜索 |
| `storageFormat` | keyword | 存储格式（PARQUET/ORC/CSV） | 精确匹配 |
| `storageLocation` | keyword | 存储位置 | 精确匹配 |
| `dataSizeBytes` | long | 数据大小（字节） | 范围查询 |
| `ownerUsername` | keyword | 所有者用户名 | 精确匹配 |
| `ownerEmail` | keyword | 所有者邮箱 | 精确匹配 |
| `columnNames` | text | 字段名列表 | 全文搜索 |
| `columnDescriptions` | text | 字段描述列表 | 全文搜索 |
| `tags` | keyword | 标签列表 | 精确匹配 |
| `catalogPaths` | keyword | 目录路径列表 | 精确匹配 |
| `createdAt` | date | 创建时间 | 范围查询 |
| `updatedAt` | date | 更新时间 | 范围查询 |
| `lastAccessedAt` | date | 最后访问时间 | 范围查询 |
| `recordCount` | long | 记录数 | 范围查询 |
| `nullRate` | double | 空值率 | 范围查询 |
| `dataFreshnessHours` | integer | 数据新鲜度（小时） | 范围查询 |
| `isActive` | boolean | 是否活跃 | 精确匹配 |
| `searchScore` | float | 搜索评分 | 排序 |

## 核心组件

### 1. ElasticsearchConfig
配置类，负责创建 Elasticsearch 客户端和连接管理。

**功能：**
- 创建 `ElasticsearchClient` Bean
- 创建 `RestClient` Bean
- 配置连接参数（超时、认证等）
- 支持多节点集群

### 2. ElasticsearchIndexConfig
索引初始化配置类，在应用启动时自动创建索引。

**功能：**
- 检查索引是否存在
- 创建索引和映射
- 配置分片和副本数量
- 定义字段类型和分析器

### 3. TableDocument
Elasticsearch 文档实体类，对应索引中的文档结构。

**注解：**
- `@Document`: 指定索引名称
- `@Id`: 文档 ID
- `@Field`: 字段映射配置

### 4. TableDocumentRepository
Elasticsearch Repository 接口，提供查询方法。

**查询方法：**
- `findByDatabaseName`: 按数据库名查询
- `findByTableType`: 按表类型查询
- `findByOwnerUsername`: 按所有者查询
- `findByTagsContaining`: 按标签查询
- `findByUpdatedAtBetween`: 按时间范围查询
- `searchByKeyword`: 全文搜索

### 5. TableDocumentMapper
转换工具类，负责在 `TableMetadata` 和 `TableDocument` 之间转换。

## 使用示例

### 1. 索引表元数据

```java
@Service
@RequiredArgsConstructor
public class SearchService {
    private final TableDocumentRepository tableDocumentRepository;
    
    public void indexTable(TableMetadata table) {
        TableDocument document = TableDocumentMapper.toDocument(table);
        tableDocumentRepository.save(document);
    }
}
```

### 2. 全文搜索

```java
public Page<TableDocument> search(String keyword, Pageable pageable) {
    return tableDocumentRepository.searchByKeyword(keyword, pageable);
}
```

### 3. 过滤查询

```java
public Page<TableDocument> findByDatabase(String databaseName, Pageable pageable) {
    return tableDocumentRepository.findByDatabaseName(databaseName, pageable);
}
```

## 部署说明

### 本地开发环境

使用 Docker 启动 Elasticsearch：

```bash
docker run -d \
  --name elasticsearch \
  -p 9200:9200 \
  -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  elasticsearch:8.11.0
```

### 生产环境

1. **集群部署**：配置多个节点，提高可用性
2. **启用认证**：配置 `username` 和 `password`
3. **调整分片**：根据数据量调整 `number-of-shards`
4. **配置副本**：设置 `number-of-replicas` 至少为 1
5. **监控**：使用 Kibana 监控集群状态

## 性能优化

### 1. 索引优化
- 合理设置分片数量（建议：数据量 / 50GB）
- 使用批量索引（`saveAll`）提高写入性能
- 定期刷新索引（`refresh_interval`）

### 2. 查询优化
- 使用 `keyword` 类型进行精确匹配
- 使用 `text` 类型进行全文搜索
- 合理使用字段权重（`^3`, `^2`）
- 启用查询缓存

### 3. 分析器配置
- 使用 `standard` 分析器（默认）
- 可选：配置中文分词器（IK Analyzer）

## 故障排查

### 1. 连接失败
- 检查 Elasticsearch 服务是否启动
- 验证 `elasticsearch.uris` 配置是否正确
- 检查网络连接和防火墙设置

### 2. 索引创建失败
- 查看应用日志中的错误信息
- 检查 Elasticsearch 集群状态
- 验证索引映射配置是否正确

### 3. 搜索结果不准确
- 检查字段映射类型（text vs keyword）
- 调整字段权重
- 验证分析器配置

## 监控和维护

### 健康检查
```bash
curl http://localhost:9200/_cluster/health
```

### 查看索引信息
```bash
curl http://localhost:9200/metadata_tables
```

### 查看索引映射
```bash
curl http://localhost:9200/metadata_tables/_mapping
```

### 删除索引（谨慎操作）
```bash
curl -X DELETE http://localhost:9200/metadata_tables
```

## 参考资料

- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Spring Data Elasticsearch](https://docs.spring.io/spring-data/elasticsearch/docs/current/reference/html/)
- [Elasticsearch Java Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
