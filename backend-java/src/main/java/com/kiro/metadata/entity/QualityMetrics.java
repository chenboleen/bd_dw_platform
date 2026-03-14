package com.kiro.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 数据质量指标实体类
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("quality_metrics")
public class QualityMetrics {
    
    /**
     * 指标ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 表ID
     */
    @NotNull(message = "表ID不能为空")
    @TableField("table_id")
    private Long tableId;
    
    /**
     * 记录数
     */
    @TableField("record_count")
    private Long recordCount;
    
    /**
     * 空值率(0-1)
     */
    @DecimalMin(value = "0.0", message = "空值率不能小于0")
    @DecimalMax(value = "1.0", message = "空值率不能大于1")
    @TableField("null_rate")
    private BigDecimal nullRate;
    
    /**
     * 更新频率 (DAILY/WEEKLY/MONTHLY)
     */
    @TableField("update_frequency")
    private String updateFrequency;
    
    /**
     * 数据新鲜度(小时)
     */
    @TableField("data_freshness_hours")
    private Integer dataFreshnessHours;
    
    /**
     * 测量时间
     */
    @NotNull(message = "测量时间不能为空")
    @TableField("measured_at")
    private LocalDateTime measuredAt;
    
    /**
     * 创建时间 - 自动填充
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
