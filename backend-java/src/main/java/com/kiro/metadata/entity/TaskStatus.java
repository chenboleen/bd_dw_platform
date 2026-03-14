package com.kiro.metadata.entity;

/**
 * 任务状态枚举
 * 
 * @author Kiro
 */
public enum TaskStatus {
    /**
     * 等待中
     */
    PENDING("等待中"),
    
    /**
     * 运行中
     */
    RUNNING("运行中"),
    
    /**
     * 已完成
     */
    COMPLETED("已完成"),
    
    /**
     * 失败
     */
    FAILED("失败");
    
    private final String description;
    
    TaskStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
