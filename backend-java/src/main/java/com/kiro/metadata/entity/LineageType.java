package com.kiro.metadata.entity;

/**
 * 血缘类型枚举
 * 
 * @author Kiro
 */
public enum LineageType {
    /**
     * 直接血缘关系
     */
    DIRECT("直接血缘"),
    
    /**
     * 间接血缘关系
     */
    INDIRECT("间接血缘");
    
    private final String description;
    
    LineageType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
