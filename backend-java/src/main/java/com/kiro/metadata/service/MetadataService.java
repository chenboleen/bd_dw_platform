package com.kiro.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kiro.metadata.document.TableDocument;
import com.kiro.metadata.entity.ChangeHistory;
import com.kiro.metadata.entity.OperationType;
import com.kiro.metadata.entity.TableMetadata;
import com.kiro.metadata.repository.CatalogRepository;
import com.kiro.metadata.repository.ChangeHistoryRepository;
import com.kiro.metadata.repository.TableRepository;
import com.kiro.metadata.util.TableDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表元数据服务
 * 提供表的CRUD操作、缓存管理和Elasticsearch同步
 * 
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetadataService {
    
    private final TableRepository tableRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SearchService searchService;
    private final CatalogRepository catalogRepository;
    
    private static final String CACHE_KEY_PREFIX = "table:";
    private static final long CACHE_TTL_HOURS = 1;
    
    /**
     * 创建表元数据
     * 
     * @param table 表元数据
     * @return 创建的表
     */
    @Transactional(rollbackFor = Exception.class)
    public TableMetadata createTable(TableMetadata table) {
        log.info("创建表元数据: {}.{}", table.getDatabaseName(), table.getTableName());
        
        // 验证表名唯一性
        QueryWrapper<TableMetadata> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("database_name", table.getDatabaseName())
                   .eq("table_name", table.getTableName());
        
        if (tableRepository.selectCount(queryWrapper) > 0) {
            throw new IllegalArgumentException(
                String.format("表 %s.%s 已存在", table.getDatabaseName(), table.getTableName())
            );
        }
        
        // 保存表元数据
        tableRepository.insert(table);
        
        // 记录变更历史
        recordChange(table.getId(), OperationType.CREATE, null, null, 
                    table.toString(), table.getOwnerId());
        
        // 同步到Elasticsearch
        try {
            TableDocument document = TableDocumentMapper.toDocument(table);
            searchService.indexTable(document);
        } catch (Exception e) {
            log.error("同步表到Elasticsearch失败: {}", e.getMessage(), e);
        }
        
        log.info("表元数据创建成功, ID: {}", table.getId());
        return table;
    }
    
    /**
     * 根据ID获取表元数据(带缓存)
     * 
     * @param tableId 表ID
     * @return 表元数据
     */
    @Cacheable(value = "tables", key = "#tableId", unless = "#result == null")
    public TableMetadata getTableById(Long tableId) {
        log.debug("查询表元数据, ID: {}", tableId);
        
        TableMetadata table = tableRepository.selectById(tableId);
        if (table == null) {
            throw new IllegalArgumentException("表不存在, ID: " + tableId);
        }
        
        return table;
    }
    
    /**
     * 根据数据库名和表名获取表元数据
     * 
     * @param databaseName 数据库名
     * @param tableName 表名
     * @return 表元数据
     */
    public TableMetadata getTableByName(String databaseName, String tableName) {
        log.debug("查询表元数据: {}.{}", databaseName, tableName);
        
        QueryWrapper<TableMetadata> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("database_name", databaseName)
                   .eq("table_name", tableName);
        
        TableMetadata table = tableRepository.selectOne(queryWrapper);
        if (table == null) {
            throw new IllegalArgumentException(
                String.format("表不存在: %s.%s", databaseName, tableName)
            );
        }
        
        return table;
    }
    
    /**
     * 查询表列表(分页)
     * 
     * @param filters 过滤条件（支持 keyword 模糊搜索表名+描述，catalogId 数据域过滤）
     * @param page 页码
     * @param pageSize 每页大小
     * @param sortBy 排序字段
     * @param sortOrder 排序方向(asc/desc)
     * @return 分页结果
     */
    public Page<TableMetadata> listTables(Map<String, Object> filters, 
                                          int page, int pageSize, 
                                          String sortBy, String sortOrder) {
        log.debug("查询表列表, 页码: {}, 每页大小: {}", page, pageSize);
        
        // 构建查询条件
        QueryWrapper<TableMetadata> queryWrapper = new QueryWrapper<>();
        
        if (filters != null) {
            // 数据库名过滤
            if (filters.containsKey("databaseName")) {
                queryWrapper.eq("database_name", filters.get("databaseName"));
            }
            
            // 表类型过滤
            if (filters.containsKey("tableType")) {
                queryWrapper.eq("table_type", filters.get("tableType"));
            }
            
            // 所有者过滤
            if (filters.containsKey("ownerId")) {
                queryWrapper.eq("owner_id", filters.get("ownerId"));
            }
            
            // 关键词模糊查询（表名 OR 描述）
            if (filters.containsKey("keyword")) {
                String kw = filters.get("keyword").toString();
                queryWrapper.and(w -> w.like("table_name", kw).or().like("description", kw));
            }

            // 数据域过滤：先查出该 catalog 下的 tableId 列表
            if (filters.containsKey("catalogId")) {
                Long cid = Long.valueOf(filters.get("catalogId").toString());
                List<TableMetadata> tablesInCatalog = catalogRepository.getTablesInCatalog(cid);
                if (tablesInCatalog.isEmpty()) {
                    // 该数据域下无表，直接返回空结果
                    Page<TableMetadata> emptyPage = new Page<>(page, pageSize);
                    emptyPage.setTotal(0);
                    emptyPage.setRecords(java.util.Collections.emptyList());
                    return emptyPage;
                }
                List<Long> tableIds = tablesInCatalog.stream()
                    .map(TableMetadata::getId)
                    .collect(Collectors.toList());
                queryWrapper.in("id", tableIds);
            }
        }
        
        // 排序
        if (sortBy != null && !sortBy.isEmpty()) {
            String dbColumn = camelToSnake(sortBy);
            boolean isAsc = "asc".equalsIgnoreCase(sortOrder);
            queryWrapper.orderBy(true, isAsc, dbColumn);
        } else {
            // 默认按更新时间倒序
            queryWrapper.orderByDesc("updated_at");
        }
        
        // 分页查询
        Page<TableMetadata> pageParam = new Page<>(page, pageSize);
        return tableRepository.selectPage(pageParam, queryWrapper);
    }
    
    /**
     * 驼峰命名转下划线命名
     * 
     * @param camelCase 驼峰命名字符串
     * @return 下划线命名字符串
     */
    private String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toLowerCase(camelCase.charAt(0)));
        for (int i = 1; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * 更新表元数据
     * 
     * @param tableId 表ID
     * @param updates 更新内容
     * @return 更新后的表
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "tables", key = "#tableId")
    public TableMetadata updateTable(Long tableId, TableMetadata updates) {
        log.info("更新表元数据, ID: {}", tableId);
        
        // 查询原表
        TableMetadata oldTable = getTableById(tableId);
        String oldValue = oldTable.toString();
        
        // 更新字段
        if (updates.getDescription() != null) {
            oldTable.setDescription(updates.getDescription());
        }
        if (updates.getStorageFormat() != null) {
            oldTable.setStorageFormat(updates.getStorageFormat());
        }
        if (updates.getStorageLocation() != null) {
            oldTable.setStorageLocation(updates.getStorageLocation());
        }
        if (updates.getDataSizeBytes() != null) {
            oldTable.setDataSizeBytes(updates.getDataSizeBytes());
        }
        
        // 保存更新
        tableRepository.updateById(oldTable);
        
        // 记录变更历史
        recordChange(tableId, OperationType.UPDATE, null, 
                    oldValue, oldTable.toString(), oldTable.getOwnerId());
        
        // 更新Elasticsearch索引
        try {
            TableDocument document = TableDocumentMapper.toDocument(oldTable);
            searchService.updateIndex(document);
        } catch (Exception e) {
            log.error("更新Elasticsearch索引失败: {}", e.getMessage(), e);
        }
        
        log.info("表元数据更新成功, ID: {}", tableId);
        return oldTable;
    }
    
    /**
     * 删除表元数据(软删除)
     * 
     * @param tableId 表ID
     * @param userId 操作用户ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "tables", key = "#tableId")
    public boolean deleteTable(Long tableId, Long userId) {
        log.info("删除表元数据, ID: {}", tableId);
        
        // 查询表
        TableMetadata table = getTableById(tableId);
        
        // 记录变更历史
        recordChange(tableId, OperationType.DELETE, null, 
                    table.toString(), null, userId);
        
        // 删除表(物理删除)
        int result = tableRepository.deleteById(tableId);
        
        // 从Elasticsearch删除
        try {
            searchService.deleteFromIndex(tableId);
        } catch (Exception e) {
            log.error("从Elasticsearch删除索引失败: {}", e.getMessage(), e);
        }
        
        log.info("表元数据删除成功, ID: {}", tableId);
        return result > 0;
    }
    
    /**
     * 全量同步所有表数据到 Elasticsearch
     *
     * @return 同步成功数量
     */
    public int syncAllToElasticsearch() {
        log.info("开始全量同步表数据到 Elasticsearch");
        try {
            // 查询所有表（不分页）
            QueryWrapper<TableMetadata> queryWrapper = new QueryWrapper<>();
            queryWrapper.orderByAsc("id");
            List<TableMetadata> allTables = tableRepository.selectList(queryWrapper);

            if (allTables.isEmpty()) {
                log.info("MySQL 中无表数据，跳过同步");
                return 0;
            }

            // 转换为文档并批量索引
            List<TableDocument> documents = allTables.stream()
                    .map(TableDocumentMapper::toDocument)
                    .collect(java.util.stream.Collectors.toList());

            int count = searchService.bulkIndexTables(documents);
            log.info("全量同步完成，共同步 {} 条记录", count);
            return count;
        } catch (Exception e) {
            log.error("全量同步失败: {}", e.getMessage(), e);
            throw new RuntimeException("全量同步失败: " + e.getMessage(), e);
        }
    }

    /**
     * 记录变更历史
     * 
     * @param entityId 实体ID
     * @param operation 操作类型
     * @param fieldName 字段名
     * @param oldValue 旧值
     * @param newValue 新值
     * @param userId 用户ID
     */
    private void recordChange(Long entityId, OperationType operation, 
                             String fieldName, String oldValue, 
                             String newValue, Long userId) {
        try {
            ChangeHistory history = new ChangeHistory();
            history.setEntityType("TABLE");
            history.setEntityId(entityId);
            history.setOperation(operation);
            history.setFieldName(fieldName);
            history.setOldValue(oldValue);
            history.setNewValue(newValue);
            history.setChangedAt(LocalDateTime.now());
            history.setChangedBy(userId);
            
            changeHistoryRepository.insert(history);
        } catch (Exception e) {
            log.error("记录变更历史失败: {}", e.getMessage(), e);
        }
    }
}
