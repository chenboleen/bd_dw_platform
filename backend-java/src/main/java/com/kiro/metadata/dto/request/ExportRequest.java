package com.kiro.metadata.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 导出请求 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {
    
    /**
     * 导出类型(CSV/JSON)
     */
    @NotBlank(message = "导出类型不能为空")
    private String exportType;
    
    /**
     * 过滤条件(JSON格式)
     */
    private Map<String, Object> filters;
    
    /**
     * 是否包含字段信息
     */
    private Boolean includeColumns = true;
}
