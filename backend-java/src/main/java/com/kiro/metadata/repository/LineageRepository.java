package com.kiro.metadata.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kiro.metadata.entity.Lineage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 血缘关系 Repository
 * 
 * @author Kiro
 */
@Mapper
public interface LineageRepository extends BaseMapper<Lineage> {
    
    /**
     * 查询指定表的上游表
     * 
     * @param targetTableId 目标表ID
     * @return 血缘关系列表
     */
    default List<Lineage> findUpstreamByTableId(Long targetTableId) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Lineage>()
                .eq(Lineage::getTargetTableId, targetTableId)
        );
    }
    
    /**
     * 查询指定表的下游表
     * 
     * @param sourceTableId 源表ID
     * @return 血缘关系列表
     */
    default List<Lineage> findDownstreamByTableId(Long sourceTableId) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Lineage>()
                .eq(Lineage::getSourceTableId, sourceTableId)
        );
    }
    
    /**
     * 查询指定表的所有血缘关系(上游+下游)
     * 
     * @param tableId 表ID
     * @return 血缘关系列表
     */
    default List<Lineage> findAllByTableId(Long tableId) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Lineage>()
                .eq(Lineage::getSourceTableId, tableId)
                .or()
                .eq(Lineage::getTargetTableId, tableId)
        );
    }
}
