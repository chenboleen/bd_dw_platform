package com.kiro.metadata.entity;

/**
 * 用户角色枚举
 * 
 * @author Kiro
 */
public enum UserRole {
    /**
     * 管理员 - 拥有所有权限
     */
    ADMIN("管理员"),
    
    /**
     * 开发人员 - 可以查询和编辑元数据注释
     */
    DEVELOPER("开发人员"),
    
    /**
     * 访客 - 只能查询公开的元数据信息
     */
    GUEST("访客");
    
    private final String description;
    
    UserRole(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
