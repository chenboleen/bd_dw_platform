package com.kiro.metadata.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 资源重复异常
 * 当创建的资源已存在时抛出(如表名重复、目录名重复等)
 * 
 * @author Kiro
 */
public class DuplicateResourceException extends BusinessException {
    
    /**
     * 构造资源重复异常
     * 
     * @param message 错误消息
     */
    public DuplicateResourceException(String message) {
        super("DUPLICATE_RESOURCE", message, HttpStatus.CONFLICT);
    }
    
    /**
     * 构造资源重复异常(带资源类型和标识)
     * 
     * @param resourceType 资源类型
     * @param identifier 资源标识
     */
    public DuplicateResourceException(String resourceType, String identifier) {
        super("DUPLICATE_RESOURCE", 
              String.format("%s '%s' 已存在", resourceType, identifier), 
              HttpStatus.CONFLICT,
              Map.of("resource_type", resourceType, "identifier", identifier));
    }
}
