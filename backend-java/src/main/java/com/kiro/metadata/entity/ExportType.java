package com.kiro.metadata.entity;

/**
 * 导出类型枚举
 * 
 * @author Kiro
 */
public enum ExportType {
    /**
     * CSV格式
     */
    CSV("CSV格式"),
    
    /**
     * JSON格式
     */
    JSON("JSON格式");
    
    private final String description;
    
    ExportType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
