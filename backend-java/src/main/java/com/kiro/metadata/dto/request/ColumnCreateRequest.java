package com.kiro.metadata.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字段创建请求 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnCreateRequest {
    
    /**
     * 表ID
     */
    @NotNull(message = "表ID不能为空")
    private Long tableId;
    
    /**
     * 字段名
     */
    @NotBlank(message = "字段名不能为空")
    private String columnName;
    
    /**
     * 数据类型
     */
    @NotBlank(message = "数据类型不能为空")
    private String dataType;
    
    /**
     * 字段顺序
     */
    @NotNull(message = "字段顺序不能为空")
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
}
