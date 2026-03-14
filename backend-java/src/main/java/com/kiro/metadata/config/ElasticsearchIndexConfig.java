package com.kiro.metadata.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.DateProperty;
import co.elastic.clients.elasticsearch._types.mapping.BooleanProperty;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import com.kiro.metadata.service.MetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Elasticsearch 索引初始化配置
 * 在应用启动时自动创建索引和映射
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexConfig {

    private final ElasticsearchClient elasticsearchClient;
    private final MetadataService metadataService;

    @Value("${elasticsearch.index.name}")
    private String indexName;

    @Value("${elasticsearch.index.number-of-shards:1}")
    private Integer numberOfShards;

    @Value("${elasticsearch.index.number-of-replicas:1}")
    private Integer numberOfReplicas;

    /**
     * 应用启动后初始化索引
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {
        try {
            log.info("开始初始化 Elasticsearch 索引: {}", indexName);

            // 检查索引是否存在
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(indexName)))
                    .value();

            if (exists) {
                log.info("索引 {} 已存在，跳过创建", indexName);
            } else {
                // 创建索引映射
                Map<String, Property> properties = createIndexMapping();

                // 创建索引
                elasticsearchClient.indices().create(CreateIndexRequest.of(c -> c
                        .index(indexName)
                        .settings(IndexSettings.of(s -> s
                                .numberOfShards(String.valueOf(numberOfShards))
                                .numberOfReplicas(String.valueOf(numberOfReplicas))
                                .refreshInterval(t -> t.time("1s"))
                        ))
                        .mappings(m -> m.properties(properties))
                ));

                log.info("成功创建 Elasticsearch 索引: {}", indexName);
            }

            // 全量同步数据到 Elasticsearch
            log.info("开始全量同步数据到 Elasticsearch");
            int syncedCount = metadataService.syncAllToElasticsearch();
            log.info("全量同步完成，共同步 {} 条记录", syncedCount);

        } catch (Exception e) {
            log.error("初始化 Elasticsearch 索引失败: {}", e.getMessage(), e);
            // 不抛出异常，避免影响应用启动
        }
    }

    /**
     * 创建索引映射
     *
     * @return 字段映射配置
     */
    private Map<String, Property> createIndexMapping() {
        Map<String, Property> properties = new HashMap<>();

        // id - 表 ID（keyword）
        properties.put("id", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));

        // databaseName - 数据库名（keyword，支持精确匹配）
        properties.put("databaseName", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));

        // tableName - 表名（text + keyword，支持全文搜索和精确匹配）
        properties.put("tableName", Property.of(p -> p.text(TextProperty.of(t -> t
                .analyzer("standard")
                .fields("keyword", Property.of(f -> f.keyword(KeywordProperty.of(k -> k))))
        ))));

        // tableType - 表类型（keyword）
        properties.put("tableType", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));

        // description - 表描述（text，支持全文搜索）
        properties.put("description", Property.of(p -> p.text(TextProperty.of(t -> t
                .analyzer("standard")
        ))));

        // storageFormat - 存储格式（keyword）
        properties.put("storageFormat", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));

        // storageLocation - 存储位置（keyword）
        properties.put("storageLocation", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));

        // dataSizeBytes - 数据大小（long）
        properties.put("dataSizeBytes", Property.of(p -> p.long_(l -> l)));

        // ownerUsername - 所有者用户名（keyword）
        properties.put("ownerUsername", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));

        // ownerEmail - 所有者邮箱（keyword）
        properties.put("ownerEmail", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));

        // columnNames - 字段名列表（text，支持全文搜索）
        properties.put("columnNames", Property.of(p -> p.text(TextProperty.of(t -> t
                .analyzer("standard")
        ))));

        // columnDescriptions - 字段描述列表（text，支持全文搜索）
        properties.put("columnDescriptions", Property.of(p -> p.text(TextProperty.of(t -> t
                .analyzer("standard")
        ))));

        // tags - 标签列表（keyword）
        properties.put("tags", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));

        // catalogPaths - 目录路径列表（keyword）
        properties.put("catalogPaths", Property.of(p -> p.keyword(KeywordProperty.of(k -> k))));

        // createdAt - 创建时间（date）
        properties.put("createdAt", Property.of(p -> p.date(DateProperty.of(d -> d
                .format("uuuu-MM-dd'T'HH:mm:ss")
        ))));

        // updatedAt - 更新时间（date）
        properties.put("updatedAt", Property.of(p -> p.date(DateProperty.of(d -> d
                .format("uuuu-MM-dd'T'HH:mm:ss")
        ))));

        // lastAccessedAt - 最后访问时间（date）
        properties.put("lastAccessedAt", Property.of(p -> p.date(DateProperty.of(d -> d
                .format("uuuu-MM-dd'T'HH:mm:ss")
        ))));

        // recordCount - 记录数（long）
        properties.put("recordCount", Property.of(p -> p.long_(l -> l)));

        // nullRate - 空值率（double）
        properties.put("nullRate", Property.of(p -> p.double_(d -> d)));

        // dataFreshnessHours - 数据新鲜度（integer）
        properties.put("dataFreshnessHours", Property.of(p -> p.integer(i -> i)));

        // isActive - 是否活跃（boolean）
        properties.put("isActive", Property.of(p -> p.boolean_(BooleanProperty.of(b -> b))));

        // searchScore - 搜索评分（float）
        properties.put("searchScore", Property.of(p -> p.float_(f -> f)));

        log.debug("创建索引映射，共 {} 个字段", properties.size());
        return properties;
    }
}