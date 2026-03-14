package com.kiro.metadata.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段更新请求 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnUpdateRequest {
    
    /**
     * 数据类型
     */
    private String dataType;
    
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
}
