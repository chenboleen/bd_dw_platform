package com.kiro.metadata.entity;

/**
 * 操作类型枚举
 * 
 * @author Kiro
 */
public enum OperationType {
    /**
     * 创建操作
     */
    CREATE("创建"),
    
    /**
     * 更新操作
     */
    UPDATE("更新"),
    
    /**
     * 删除操作
     */
    DELETE("删除");
    
    private final String description;
    
    OperationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
