package com.kiro.metadata.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 表元数据实体类
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tables")
public class TableMetadata {
    
    /**
     * 表ID - 主键自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 数据库名
     */
    @NotBlank(message = "数据库名不能为空")
    @TableField("database_name")
    private String databaseName;
    
    /**
     * 表名
     */
    @NotBlank(message = "表名不能为空")
    @TableField("table_name")
    private String tableName;
    
    /**
     * 表类型
     */
    @NotNull(message = "表类型不能为空")
    @TableField("table_type")
    private TableType tableType;
    
    /**
     * 表描述
     */
    @TableField("description")
    private String description;
    
    /**
     * 存储格式 (PARQUET/ORC/CSV)
     */
    @TableField("storage_format")
    private String storageFormat;
    
    /**
     * 存储位置
     */
    @TableField("storage_location")
    private String storageLocation;
    
    /**
     * 数据大小(字节)
     */
    @TableField("data_size_bytes")
    private Long dataSizeBytes;
    
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
     * 最后访问时间
     */
    @TableField("last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    /**
     * 所有者ID
     */
    @NotNull(message = "所有者ID不能为空")
    @TableField("owner_id")
    private Long ownerId;
}
