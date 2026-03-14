package com.kiro.metadata.handler;

import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 元数据自动填充处理器
 * 自动填充创建时间和更新时间
 * 
 * @author Kiro
 */
@Component
public class MetaObjectHandler implements com.baomidou.mybatisplus.core.handlers.MetaObjectHandler {
    
    /**
     * 插入时自动填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        
        // 自动填充创建时间
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        
        // 自动填充更新时间
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }
    
    /**
     * 更新时自动填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 自动填充更新时间
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
