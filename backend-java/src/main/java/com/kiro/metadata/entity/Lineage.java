package com.kiro.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 血缘关系实体类
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("lineage")
public class Lineage {
    
    /**
     * 血缘ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 上游表ID
     */
    @NotNull(message = "上游表ID不能为空")
    @TableField("source_table_id")
    private Long sourceTableId;
    
    /**
     * 下游表ID
     */
    @NotNull(message = "下游表ID不能为空")
    @TableField("target_table_id")
    private Long targetTableId;
    
    /**
     * 血缘类型
     */
    @NotNull(message = "血缘类型不能为空")
    @TableField("lineage_type")
    private LineageType lineageType;
    
    /**
     * 转换逻辑
     */
    @TableField("transformation_logic")
    private String transformationLogic;
    
    /**
     * 创建时间 - 自动填充
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间 - 自动填充
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    @TableField("created_by")
    private Long createdBy;
}
