package com.kiro.metadata.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kiro.metadata.entity.ColumnMetadata;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 字段元数据 Repository
 * 
 * @author Kiro
 */
@Mapper
public interface ColumnRepository extends BaseMapper<ColumnMetadata> {
    
    /**
     * 根据表ID查询所有字段
     * 
     * @param tableId 表ID
     * @return 字段列表
     */
    default List<ColumnMetadata> findByTableId(Long tableId) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ColumnMetadata>()
                .eq(ColumnMetadata::getTableId, tableId)
                .orderByAsc(ColumnMetadata::getColumnOrder)
        );
    }
    
    /**
     * 根据表ID和字段名查询字段
     * 
     * @param tableId 表ID
     * @param columnName 字段名
     * @return 字段元数据
     */
    default ColumnMetadata findByTableIdAndColumnName(Long tableId, String columnName) {
        return selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ColumnMetadata>()
                .eq(ColumnMetadata::getTableId, tableId)
                .eq(ColumnMetadata::getColumnName, columnName)
        );
    }
    
    /**
     * 根据表ID删除所有字段
     * 
     * @param tableId 表ID
     * @return 删除数量
     */
    default int deleteByTableId(Long tableId) {
        return delete(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ColumnMetadata>()
                .eq(ColumnMetadata::getTableId, tableId)
        );
    }
}
