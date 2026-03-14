package com.kiro.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 变更历史实体类
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("change_history")
public class ChangeHistory {
    
    /**
     * 历史ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 实体类型 (TABLE/COLUMN/CATALOG)
     */
    @NotBlank(message = "实体类型不能为空")
    @TableField("entity_type")
    private String entityType;
    
    /**
     * 实体ID
     */
    @NotNull(message = "实体ID不能为空")
    @TableField("entity_id")
    private Long entityId;
    
    /**
     * 操作类型
     */
    @NotNull(message = "操作类型不能为空")
    @TableField("operation")
    private OperationType operation;
    
    /**
     * 字段名
     */
    @TableField("field_name")
    private String fieldName;
    
    /**
     * 旧值(JSON字符串)
     */
    @TableField("old_value")
    private String oldValue;
    
    /**
     * 新值(JSON字符串)
     */
    @TableField("new_value")
    private String newValue;
    
    /**
     * 变更时间
     */
    @NotNull(message = "变更时间不能为空")
    @TableField("changed_at")
    private LocalDateTime changedAt;
    
    /**
     * 变更人ID
     */
    @NotNull(message = "变更人ID不能为空")
    @TableField("changed_by")
    private Long changedBy;
}
