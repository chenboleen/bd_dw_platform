package com.kiro.metadata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据质量指标响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityMetricsResponse {
    
    /**
     * 指标ID
     */
    private Long id;
    
    /**
     * 表ID
     */
    private Long tableId;
    
    /**
     * 记录数
     */
    private Long recordCount;
    
    /**
     * 空值率(0-1)
     */
    private BigDecimal nullRate;
    
    /**
     * 更新频率
     */
    private String updateFrequency;
    
    /**
     * 数据新鲜度(小时)
     */
    private Integer dataFreshnessHours;
    
    /**
     * 测量时间
     */
    private LocalDateTime measuredAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
