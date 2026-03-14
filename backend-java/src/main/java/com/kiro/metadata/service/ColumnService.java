package com.kiro.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kiro.metadata.entity.ChangeHistory;
import com.kiro.metadata.entity.ColumnMetadata;
import com.kiro.metadata.entity.OperationType;
import com.kiro.metadata.repository.ChangeHistoryRepository;
import com.kiro.metadata.repository.ColumnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 字段元数据服务
 * 提供字段的CRUD操作和缓存管理
 * 
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColumnService {
    
    private final ColumnRepository columnRepository;
    private final ChangeHistoryRepository changeHistoryRepository;
    
    /**
     * 创建字段元数据
     * 
     * @param column 字段元数据
     * @param userId 操作用户ID
     * @return 创建的字段
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "columns", key = "'table:' + #column.tableId")
    public ColumnMetadata createColumn(ColumnMetadata column, Long userId) {
        log.info("创建字段元数据: {}.{}", column.getTableId(), column.getColumnName());
        
        // 保存字段元数据
        columnRepository.insert(column);
        
        // 记录变更历史
        recordChange(column.getId(), OperationType.CREATE, null, 
                    null, column.toString(), userId);
        
        log.info("字段元数据创建成功, ID: {}", column.getId());
        return column;
    }
    
    /**
     * 根据表ID获取字段列表(带缓存)
     * 
     * @param tableId 表ID
     * @return 字段列表
     */
    @Cacheable(value = "columns", key = "'table:' + #tableId")
    public List<ColumnMetadata> getColumnsByTableId(Long tableId) {
        log.debug("查询表的字段列表, 表ID: {}", tableId);
        
        QueryWrapper<ColumnMetadata> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("table_id", tableId)
                   .orderByAsc("column_order");
        
        return columnRepository.selectList(queryWrapper);
    }
    
    /**
     * 更新字段元数据
     * 
     * @param columnId 字段ID
     * @param updates 更新内容
     * @param userId 操作用户ID
     * @return 更新后的字段
     */
    @Transactional(rollbackFor = Exception.class)
    public ColumnMetadata updateColumn(Long columnId, ColumnMetadata updates, Long userId) {
        log.info("更新字段元数据, ID: {}", columnId);
        
        // 查询原字段
        ColumnMetadata oldColumn = columnRepository.selectById(columnId);
        if (oldColumn == null) {
            throw new IllegalArgumentException("字段不存在, ID: " + columnId);
        }
        
        String oldValue = oldColumn.toString();
        
        // 更新字段
        if (updates.getColumnName() != null) {
            oldColumn.setColumnName(updates.getColumnName());
        }
        if (updates.getDataType() != null) {
            oldColumn.setDataType(updates.getDataType());
        }
        if (updates.getIsNullable() != null) {
            oldColumn.setIsNullable(updates.getIsNullable());
        }
        if (updates.getIsPartitionKey() != null) {
            oldColumn.setIsPartitionKey(updates.getIsPartitionKey());
        }
        if (updates.getDescription() != null) {
            oldColumn.setDescription(updates.getDescription());
        }
        
        // 保存更新
        columnRepository.updateById(oldColumn);
        
        // 清除表的字段缓存
        evictTableColumnsCache(oldColumn.getTableId());
        
        // 记录变更历史
        recordChange(columnId, OperationType.UPDATE, null, 
                    oldValue, oldColumn.toString(), userId);
        
        log.info("字段元数据更新成功, ID: {}", columnId);
        return oldColumn;
    }
    
    /**
     * 删除字段元数据
     * 
     * @param columnId 字段ID
     * @param userId 操作用户ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteColumn(Long columnId, Long userId) {
        log.info("删除字段元数据, ID: {}", columnId);
        
        // 查询字段
        ColumnMetadata column = columnRepository.selectById(columnId);
        if (column == null) {
            throw new IllegalArgumentException("字段不存在, ID: " + columnId);
        }
        
        Long tableId = column.getTableId();
        
        // 记录变更历史
        recordChange(columnId, OperationType.DELETE, null, 
                    column.toString(), null, userId);
        
        // 删除字段
        int result = columnRepository.deleteById(columnId);
        
        // 清除表的字段缓存
        evictTableColumnsCache(tableId);
        
        log.info("字段元数据删除成功, ID: {}", columnId);
        return result > 0;
    }
    
    /**
     * 批量调整字段顺序
     * 
     * @param tableId 表ID
     * @param columnOrders 字段ID和顺序的映射
     * @param userId 操作用户ID
     * @return 是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "columns", key = "'table:' + #tableId")
    public boolean reorderColumns(Long tableId, List<Long> columnOrders, Long userId) {
        log.info("调整字段顺序, 表ID: {}", tableId);
        
        for (int i = 0; i < columnOrders.size(); i++) {
            Long columnId = columnOrders.get(i);
            ColumnMetadata column = columnRepository.selectById(columnId);
            
            if (column == null || !column.getTableId().equals(tableId)) {
                throw new IllegalArgumentException("字段不存在或不属于该表, ID: " + columnId);
            }
            
            // 更新顺序
            column.setColumnOrder(i + 1);
            columnRepository.updateById(column);
        }
        
        log.info("字段顺序调整成功, 表ID: {}", tableId);
        return true;
    }
    
    /**
     * 更新字段描述
     * 
     * @param columnId 字段ID
     * @param description 新描述
     * @param userId 操作用户ID
     * @return 更新后的字段
     */
    @Transactional(rollbackFor = Exception.class)
    public ColumnMetadata updateColumnDescription(Long columnId, String description, Long userId) {
        log.info("更新字段描述, ID: {}", columnId);
        
        ColumnMetadata column = columnRepository.selectById(columnId);
        if (column == null) {
            throw new IllegalArgumentException("字段不存在, ID: " + columnId);
        }
        
        String oldValue = column.getDescription();
        column.setDescription(description);
        columnRepository.updateById(column);
        
        // 清除表的字段缓存
        evictTableColumnsCache(column.getTableId());
        
        // 记录变更历史
        recordChange(columnId, OperationType.UPDATE, "description", 
                    oldValue, description, userId);
        
        log.info("字段描述更新成功, ID: {}", columnId);
        return column;
    }
    
    /**
     * 清除表的字段缓存
     * 
     * @param tableId 表ID
     */
    @CacheEvict(value = "columns", key = "'table:' + #tableId")
    private void evictTableColumnsCache(Long tableId) {
        log.debug("清除表的字段缓存, 表ID: {}", tableId);
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
            history.setEntityType("COLUMN");
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
