package com.kiro.metadata.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.kiro.metadata.entity.QualityMetrics;
import com.kiro.metadata.entity.TableMetadata;
import com.kiro.metadata.repository.QualityMetricsRepository;
import com.kiro.metadata.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据质量服务
 * 提供数据质量指标的记录、查询和趋势分析
 * 
 * @author Kiro
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityService {
    
    private final QualityMetricsRepository qualityMetricsRepository;
    private final TableRepository tableRepository;
    
    /**
     * 记录质量指标
     * 
     * @param metrics 质量指标
     * @return 记录的指标
     */
    @Transactional(rollbackFor = Exception.class)
    public QualityMetrics recordQualityMetrics(QualityMetrics metrics) {
        log.info("记录质量指标, 表ID: {}", metrics.getTableId());
        
        // 验证表存在
        TableMetadata table = tableRepository.selectById(metrics.getTableId());
        if (table == null) {
            throw new IllegalArgumentException("表不存在, ID: " + metrics.getTableId());
        }
        
        // 设置测量时间
        if (metrics.getMeasuredAt() == null) {
            metrics.setMeasuredAt(LocalDateTime.now());
        }
        
        // 保存指标
        qualityMetricsRepository.insert(metrics);
        
        log.info("质量指标记录成功, ID: {}", metrics.getId());
        return metrics;
    }
    
    /**
     * 获取表的最新质量指标
     * 
     * @param tableId 表ID
     * @return 质量指标
     */
    public QualityMetrics getQualityMetrics(Long tableId) {
        log.debug("查询最新质量指标, 表ID: {}", tableId);
        
        QueryWrapper<QualityMetrics> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("table_id", tableId)
                   .orderByDesc("measured_at")
                   .last("LIMIT 1");
        
        QualityMetrics metrics = qualityMetricsRepository.selectOne(queryWrapper);
        if (metrics == null) {
            log.warn("未找到质量指标, 表ID: {}", tableId);
        }
        
        return metrics;
    }
    
    /**
     * 获取质量趋势
     * 
     * @param tableId 表ID
     * @param days 天数
     * @return 质量快照列表
     */
    public List<QualityMetrics> getQualityTrend(Long tableId, int days) {
        log.info("查询质量趋势, 表ID: {}, 天数: {}", tableId, days);
        
        // 计算起始时间
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        
        QueryWrapper<QualityMetrics> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("table_id", tableId)
                   .ge("measured_at", startTime)
                   .orderByAsc("measured_at");
        
        List<QualityMetrics> trend = qualityMetricsRepository.selectList(queryWrapper);
        
        log.info("找到{}条质量快照", trend.size());
        return trend;
    }
    
    /**
     * 计算质量分数
     * 
     * @param metrics 质量指标
     * @return 质量分数(0-100)
     */
    public double calculateQualityScore(QualityMetrics metrics) {
        log.debug("计算质量分数, 指标ID: {}", metrics.getId());
        
        double score = 100.0;
        
        // 空值率扣分(空值率越高,扣分越多)
        if (metrics.getNullRate() != null) {
            score -= metrics.getNullRate().doubleValue() * 30;  // 最多扣30分
        }
        
        // 数据新鲜度扣分(数据越旧,扣分越多)
        if (metrics.getDataFreshnessHours() != null) {
            if (metrics.getDataFreshnessHours() > 24) {
                score -= Math.min(20, (metrics.getDataFreshnessHours() - 24) / 24.0 * 5);  // 最多扣20分
            }
        }
        
        // 记录数扣分(记录数为0扣分)
        if (metrics.getRecordCount() != null && metrics.getRecordCount() == 0) {
            score -= 20;
        }
        
        // 更新频率扣分(更新频率低扣分)
        if (metrics.getUpdateFrequency() != null) {
            switch (metrics.getUpdateFrequency()) {
                case "DAILY":
                    // 不扣分
                    break;
                case "WEEKLY":
                    score -= 5;
                    break;
                case "MONTHLY":
                    score -= 10;
                    break;
                default:
                    score -= 15;
                    break;
            }
        }
        
        // 确保分数在0-100之间
        score = Math.max(0, Math.min(100, score));
        
        log.debug("质量分数: {}", score);
        return score;
    }
    
    /**
     * 批量记录质量指标
     * 
     * @param metricsList 质量指标列表
     * @return 成功数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchRecordQualityMetrics(List<QualityMetrics> metricsList) {
        log.info("批量记录质量指标, 数量: {}", metricsList.size());
        
        int successCount = 0;
        for (QualityMetrics metrics : metricsList) {
            try {
                recordQualityMetrics(metrics);
                successCount++;
            } catch (Exception e) {
                log.error("记录质量指标失败, 表ID: {}", metrics.getTableId(), e);
            }
        }
        
        log.info("批量记录完成, 成功: {}, 失败: {}", successCount, metricsList.size() - successCount);
        return successCount;
    }
    
    /**
     * 获取所有表的最新质量指标
     * 
     * @return 质量指标列表
     */
    public List<QualityMetrics> getAllLatestQualityMetrics() {
        log.info("查询所有表的最新质量指标");
        
        // 使用原生SQL查询每个表的最新指标
        return qualityMetricsRepository.selectLatestMetricsForAllTables();
    }
}
