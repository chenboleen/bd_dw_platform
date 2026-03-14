package com.kiro.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 字段元数据实体类
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("columns")
public class ColumnMetadata {
    
    /**
     * 字段ID - 主键自增
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
     * 字段名
     */
    @NotBlank(message = "字段名不能为空")
    @TableField("column_name")
    private String columnName;
    
    /**
     * 数据类型
     */
    @NotBlank(message = "数据类型不能为空")
    @TableField("data_type")
    private String dataType;
    
    /**
     * 字段顺序
     */
    @NotNull(message = "字段顺序不能为空")
    @TableField("column_order")
    private Integer columnOrder;
    
    /**
     * 是否可为空
     */
    @TableField("is_nullable")
    private Boolean isNullable;
    
    /**
     * 是否分区键
     */
    @TableField("is_partition_key")
    private Boolean isPartitionKey;
    
    /**
     * 字段描述
     */
    @TableField("description")
    private String description;
    
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
}
