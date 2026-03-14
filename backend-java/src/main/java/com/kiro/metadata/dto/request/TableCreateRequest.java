package com.kiro.metadata.dto.request;

import com.kiro.metadata.entity.TableType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表创建请求 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableCreateRequest {
    
    /**
     * 数据库名
     */
    @NotBlank(message = "数据库名不能为空")
    private String databaseName;
    
    /**
     * 表名
     */
    @NotBlank(message = "表名不能为空")
    private String tableName;
    
    /**
     * 表类型
     */
    @NotNull(message = "表类型不能为空")
    private TableType tableType;
    
    /**
     * 表描述
     */
    private String description;
    
    /**
     * 存储格式
     */
    private String storageFormat;
    
    /**
     * 存储位置
     */
    private String storageLocation;
    
    /**
     * 数据大小(字节)
     */
    private Long dataSizeBytes;
    
    /**
     * 所有者ID
     */
    @NotNull(message = "所有者ID不能为空")
    private Long ownerId;
}
