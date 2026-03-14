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
    private String format;
    
    /**
     * 数据库名过滤
     */
    private String databaseName;
    
    /**
     * 表类型过滤
     */
    private String tableType;
    
    /**
     * 开始日期
     */
    private String startDate;
    
    /**
     * 结束日期
     */
    private String endDate;
    
    /**
     * 过滤条件(JSON格式)
     */
    private Map<String, Object> filters;
    
    /**
     * 是否包含字段信息
     */
    private Boolean includeColumns = true;
    
    /**
     * 获取导出类型（兼容旧代码）
     */
    public String getExportType() {
        return format;
    }
}
