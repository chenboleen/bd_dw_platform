package com.kiro.metadata.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kiro.metadata.entity.QualityMetrics;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据质量指标 Repository
 * 
 * @author Kiro
 */
@Mapper
public interface QualityMetricsRepository extends BaseMapper<QualityMetrics> {
    
    /**
     * 根据表ID查询质量指标(按测量时间倒序)
     * 
     * @param tableId 表ID
     * @return 质量指标列表
     */
    default List<QualityMetrics> findByTableId(Long tableId) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QualityMetrics>()
                .eq(QualityMetrics::getTableId, tableId)
                .orderByDesc(QualityMetrics::getMeasuredAt)
        );
    }
    
    /**
     * 查询表的最新质量指标
     * 
     * @param tableId 表ID
     * @return 最新质量指标
     */
    default QualityMetrics findLatestByTableId(Long tableId) {
        return selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QualityMetrics>()
                .eq(QualityMetrics::getTableId, tableId)
                .orderByDesc(QualityMetrics::getMeasuredAt)
                .last("LIMIT 1")
        );
    }
    
    /**
     * 查询所有表的最新质量指标
     * 
     * @return 质量指标列表
     */
    @Select("SELECT qm1.* FROM quality_metrics qm1 " +
            "INNER JOIN (" +
            "  SELECT table_id, MAX(measured_at) as max_measured_at " +
            "  FROM quality_metrics " +
            "  GROUP BY table_id" +
            ") qm2 ON qm1.table_id = qm2.table_id AND qm1.measured_at = qm2.max_measured_at")
    List<QualityMetrics> selectLatestMetricsForAllTables();
}
