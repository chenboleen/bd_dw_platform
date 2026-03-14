package com.kiro.metadata.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kiro.metadata.entity.ChangeHistory;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 变更历史 Repository
 * 
 * @author Kiro
 */
@Mapper
public interface ChangeHistoryRepository extends BaseMapper<ChangeHistory> {
    
    /**
     * 根据实体类型和实体ID查询变更历史
     * 
     * @param entityType 实体类型
     * @param entityId 实体ID
     * @return 变更历史列表
     */
    default List<ChangeHistory> findByEntityTypeAndId(String entityType, Long entityId) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChangeHistory>()
                .eq(ChangeHistory::getEntityType, entityType)
                .eq(ChangeHistory::getEntityId, entityId)
                .orderByDesc(ChangeHistory::getChangedAt)
        );
    }
    
    /**
     * 根据变更人ID查询变更历史
     * 
     * @param changedBy 变更人ID
     * @return 变更历史列表
     */
    default List<ChangeHistory> findByChangedBy(Long changedBy) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ChangeHistory>()
                .eq(ChangeHistory::getChangedBy, changedBy)
                .orderByDesc(ChangeHistory::getChangedAt)
        );
    }
}
