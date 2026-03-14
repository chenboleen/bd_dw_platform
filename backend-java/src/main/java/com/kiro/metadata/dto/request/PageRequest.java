package com.kiro.metadata.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    
    /**
     * 页码(从1开始)
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;
    
    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小必须大于0")
    private Integer size = 10;
    
    /**
     * 排序字段
     */
    private String sortBy;
    
    /**
     * 排序方向(ASC/DESC)
     */
    private String sortOrder = "ASC";
}
