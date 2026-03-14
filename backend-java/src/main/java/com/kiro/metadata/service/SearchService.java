package com.kiro.metadata.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.kiro.metadata.document.TableDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务
 * 提供 Elasticsearch 索引管理和全文搜索功能
 * 使用纯 co.elastic.clients Java 客户端，不依赖 spring-data-elasticsearch
 *
 * @author Kiro
 */
@Slf4j
@Service
public class SearchService {

    private final ElasticsearchClient elasticsearchClient;

    private final String indexName;

    public SearchService(
            ElasticsearchClient elasticsearchClient,
            @Value("${elasticsearch.index.name:metadata_tables}") String indexName) {
        this.elasticsearchClient = elasticsearchClient;
        this.indexName = indexName;
    }

    /**
     * 创建索引（如不存在则创建）
     *
     * @return 是否成功
     */
    public boolean createIndex() {
        log.info("创建 Elasticsearch 索引: {}", indexName);
        try {
            // 检查索引是否已存在
            boolean exists = elasticsearchClient.indices()
                    .exists(ExistsRequest.of(e -> e.index(indexName)))
                    .value();

            if (!exists) {
                CreateIndexResponse response = elasticsearchClient.indices()
                        .create(c -> c
                                .index(indexName)
                                .mappings(m -> m
                                        .properties("tableName", p -> p.text(t -> t.analyzer("standard")))
                                        .properties("databaseName", p -> p.keyword(k -> k))
                                        .properties("tableType", p -> p.keyword(k -> k))
                                        .properties("description", p -> p.text(t -> t.analyzer("standard")))
                                        .properties("columnNames", p -> p.text(t -> t.analyzer("standard")))
                                        .properties("tags", p -> p.keyword(k -> k))
                                        .properties("ownerId", p -> p.long_(l -> l))
                                        .properties("updatedAt", p -> p.date(d -> d))
                                )
                        );
                log.info("索引创建成功: {}, acknowledged={}", indexName, response.acknowledged());
            } else {
                log.info("索引已存在，跳过创建: {}", indexName);
            }
            return true;
        } catch (IOException e) {
            log.error("创建索引失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 索引单个表文档
     *
     * @param document 表文档
     * @return 是否成功
     */
    public boolean indexTable(TableDocument document) {
        log.debug("索引表文档, ID: {}", document.getId());
        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                    .index(indexName)
                    .id(document.getId())
                    .document(document)
            );
            log.debug("表文档索引成功, ID: {}, result: {}", document.getId(), response.result());
            return true;
        } catch (IOException e) {
            log.error("索引表文档失败, ID: {}", document.getId(), e);
            return false;
        }
    }

    /**
     * 批量索引表文档
     *
     * @param documents 表文档列表
     * @return 成功数量
     */
    public int bulkIndexTables(List<TableDocument> documents) {
        log.info("批量索引表文档, 数量: {}", documents.size());
        if (documents.isEmpty()) {
            return 0;
        }
        try {
            List<BulkOperation> operations = documents.stream()
                    .map(doc -> BulkOperation.of(b -> b
                            .index(IndexOperation.of(i -> i
                                    .index(indexName)
                                    .id(doc.getId())
                                    .document(doc)
                            ))
                    ))
                    .collect(Collectors.toList());

            BulkResponse response = elasticsearchClient.bulk(b -> b.operations(operations));

            long errorCount = response.items().stream()
                    .filter(item -> item.error() != null)
                    .count();

            int successCount = documents.size() - (int) errorCount;
            if (errorCount > 0) {
                log.warn("批量索引部分失败, 成功: {}, 失败: {}", successCount, errorCount);
            } else {
                log.info("批量索引全部成功, 数量: {}", successCount);
            }
            return successCount;
        } catch (IOException e) {
            log.error("批量索引失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 更新索引（等同于重新索引）
     *
     * @param document 表文档
     * @return 是否成功
     */
    public boolean updateIndex(TableDocument document) {
        log.debug("更新表文档索引, ID: {}", document.getId());
        return indexTable(document);
    }

    /**
     * 从索引中删除文档
     *
     * @param tableId 表ID
     * @return 是否成功
     */
    public boolean deleteFromIndex(Long tableId) {
        log.debug("删除表文档索引, ID: {}", tableId);
        try {
            DeleteResponse response = elasticsearchClient.delete(d -> d
                    .index(indexName)
                    .id(String.valueOf(tableId))
            );
            log.debug("表文档索引删除成功, ID: {}, result: {}", tableId, response.result());
            return true;
        } catch (IOException e) {
            log.error("删除表文档索引失败, ID: {}", tableId, e);
            return false;
        }
    }

    /**
     * 全文搜索
     *
     * @param keyword  关键词
     * @param filters  过滤条件
     * @param page     页码（从0开始）
     * @param pageSize 每页大小
     * @return 搜索结果
     */
    public Map<String, Object> searchTables(String keyword, Map<String, Object> filters,
                                            int page, int pageSize) {
        log.info("全文搜索, 关键词: {}, 页码: {}", keyword, page);
        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // 关键词多字段匹配
            if (keyword != null && !keyword.isEmpty()) {
                MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                        .query(keyword)
                        .fields("tableName^3", "databaseName^2", "description", "columnNames")
                        .type(TextQueryType.BestFields)
                        .fuzziness("AUTO")
                );
                boolQuery.must(Query.of(q -> q.multiMatch(multiMatchQuery)));
            }

            // 过滤条件
            applyFilters(boolQuery, filters);

            SearchResponse<TableDocument> response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .query(boolQuery.build()._toQuery())
                            .from(page * pageSize)
                            .size(pageSize)
                            .highlight(h -> h
                                    .fields("tableName", f -> f.preTags("<em>").postTags("</em>"))
                                    .fields("description", f -> f.preTags("<em>").postTags("</em>"))
                            ),
                    TableDocument.class
            );

            List<Map<String, Object>> items = new ArrayList<>();
            for (Hit<TableDocument> hit : response.hits().hits()) {
                TableDocument doc = hit.source();
                if (doc != null) {
                    Map<String, Object> item = new HashMap<>();
                    // 转换为前端期望的格式
                    try {
                        item.put("id", Long.parseLong(doc.getId()));
                    } catch (NumberFormatException e) {
                        item.put("id", doc.getId());
                    }
                    item.put("databaseName", doc.getDatabaseName());
                    item.put("tableName", doc.getTableName());
                    item.put("tableType", doc.getTableType());
                    item.put("description", doc.getDescription());
                    item.put("score", hit.score());
                    item.put("updatedAt", doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : null);
                    
                    // 处理高亮
                    Map<String, List<String>> highlights = new HashMap<>();
                    if (hit.highlight() != null) {
                        hit.highlight().forEach((key, values) -> highlights.put(key, values));
                    }
                    item.put("highlight", highlights);
                    
                    items.add(item);
                }
            }

            Map<String, Object> searchResult = new HashMap<>();
            searchResult.put("items", items);
            searchResult.put("total", response.hits().total().value());
            searchResult.put("page", page + 1); // 前端期望从1开始
            searchResult.put("pageSize", pageSize);
            searchResult.put("totalPages", (int) Math.ceil((double) response.hits().total().value() / pageSize));
            
            log.info("搜索完成, 找到 {} 条结果", response.hits().total().value());
            return searchResult;

        } catch (IOException e) {
            log.error("搜索失败: {}", e.getMessage(), e);
            throw new RuntimeException("搜索失败: " + e.getMessage(), e);
        }
    }

    /**
     * 搜索建议（自动补全）
     *
     * @param prefix 前缀
     * @param limit  最大返回数量
     * @return 建议列表
     */
    public List<String> suggest(String prefix, int limit) {
        log.debug("搜索建议, 前缀: {}", prefix);
        try {
            PrefixQuery prefixQuery = PrefixQuery.of(p -> p
                    .field("tableName")
                    .value(prefix)
            );

            SearchResponse<TableDocument> response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .query(Query.of(q -> q.prefix(prefixQuery)))
                            .size(limit)
                            .source(so -> so.filter(f -> f.includes("tableName", "databaseName"))),
                    TableDocument.class
            );

            List<String> suggestions = new ArrayList<>();
            for (Hit<TableDocument> hit : response.hits().hits()) {
                TableDocument doc = hit.source();
                if (doc != null) {
                    suggestions.add(doc.getDatabaseName() + "." + doc.getTableName());
                }
            }

            log.debug("找到 {} 条建议", suggestions.size());
            return suggestions;

        } catch (IOException e) {
            log.error("搜索建议失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 高级过滤
     *
     * @param filters  过滤条件
     * @param page     页码（从0开始）
     * @param pageSize 每页大小
     * @return 过滤结果
     */
    public Map<String, Object> filterTables(Map<String, Object> filters, int page, int pageSize) {
        log.info("高级过滤, 条件: {}", filters);
        try {
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();
            applyFilters(boolQuery, filters);

            // 更新时间范围过滤
            if (filters != null && (filters.containsKey("updatedFrom") || filters.containsKey("updatedTo"))) {
                RangeQuery.Builder rangeQuery = new RangeQuery.Builder().field("updatedAt");
                if (filters.containsKey("updatedFrom")) {
                    rangeQuery.gte(co.elastic.clients.json.JsonData.of(filters.get("updatedFrom")));
                }
                if (filters.containsKey("updatedTo")) {
                    rangeQuery.lte(co.elastic.clients.json.JsonData.of(filters.get("updatedTo")));
                }
                boolQuery.filter(Query.of(q -> q.range(rangeQuery.build())));
            }

            SearchResponse<TableDocument> response = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .query(boolQuery.build()._toQuery())
                            .from(page * pageSize)
                            .size(pageSize),
                    TableDocument.class
            );

            List<Map<String, Object>> items = new ArrayList<>();
            for (Hit<TableDocument> hit : response.hits().hits()) {
                TableDocument doc = hit.source();
                if (doc != null) {
                    Map<String, Object> item = new HashMap<>();
                    try {
                        item.put("id", Long.parseLong(doc.getId()));
                    } catch (NumberFormatException e) {
                        item.put("id", doc.getId());
                    }
                    item.put("databaseName", doc.getDatabaseName());
                    item.put("tableName", doc.getTableName());
                    item.put("tableType", doc.getTableType());
                    item.put("description", doc.getDescription());
                    item.put("updatedAt", doc.getUpdatedAt() != null ? doc.getUpdatedAt().toString() : null);
                    
                    items.add(item);
                }
            }

            Map<String, Object> filterResult = new HashMap<>();
            filterResult.put("items", items);
            filterResult.put("total", response.hits().total().value());
            filterResult.put("page", page + 1);
            filterResult.put("pageSize", pageSize);
            filterResult.put("totalPages", (int) Math.ceil((double) response.hits().total().value() / pageSize));

            log.info("过滤完成, 找到 {} 条结果", response.hits().total().value());
            return filterResult;

        } catch (IOException e) {
            log.error("过滤失败: {}", e.getMessage(), e);
            throw new RuntimeException("过滤失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将公共过滤条件应用到 BoolQuery
     */
    private void applyFilters(BoolQuery.Builder boolQuery, Map<String, Object> filters) {
        if (filters == null) {
            return;
        }
        if (filters.containsKey("databaseName")) {
            boolQuery.filter(Query.of(q -> q.term(t -> t
                    .field("databaseName")
                    .value(filters.get("databaseName").toString())
            )));
        }
        if (filters.containsKey("tableType")) {
            boolQuery.filter(Query.of(q -> q.term(t -> t
                    .field("tableType")
                    .value(filters.get("tableType").toString())
            )));
        }
        if (filters.containsKey("ownerId")) {
            boolQuery.filter(Query.of(q -> q.term(t -> t
                    .field("ownerId")
                    .value(Long.parseLong(filters.get("ownerId").toString()))
            )));
        }
    }
}
