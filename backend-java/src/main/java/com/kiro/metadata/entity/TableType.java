package com.kiro.metadata.entity;

/**
 * 表类型枚举
 * 
 * @author Kiro
 */
public enum TableType {
    /**
     * 普通表
     */
    TABLE("普通表"),
    
    /**
     * 视图
     */
    VIEW("视图"),
    
    /**
     * 外部表
     */
    EXTERNAL("外部表");
    
    private final String description;
    
    TableType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
