package com.kiro.metadata.dto.response;

import com.kiro.metadata.entity.TableType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 表响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableResponse {
    
    /**
     * 表ID
     */
    private Long id;
    
    /**
     * 数据库名
     */
    private String databaseName;
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 表类型
     */
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
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessedAt;
    
    /**
     * 所有者ID
     */
    private Long ownerId;
}
