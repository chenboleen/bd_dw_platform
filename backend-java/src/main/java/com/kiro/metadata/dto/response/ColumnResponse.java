package com.kiro.metadata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 字段响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColumnResponse {
    
    /**
     * 字段ID
     */
    private Long id;
    
    /**
     * 表ID
     */
    private Long tableId;
    
    /**
     * 字段名
     */
    private String columnName;
    
    /**
     * 数据类型
     */
    private String dataType;
    
    /**
     * 字段顺序
     */
    private Integer columnOrder;
    
    /**
     * 是否可为空
     */
    private Boolean isNullable;
    
    /**
     * 是否分区键
     */
    private Boolean isPartitionKey;
    
    /**
     * 字段描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
