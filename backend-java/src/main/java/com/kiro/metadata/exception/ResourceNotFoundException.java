package com.kiro.metadata.exception;

import org.springframework.http.HttpStatus;

/**
 * 资源不存在异常
 * 当请求的资源(表、字段、目录等)不存在时抛出
 * 
 * @author Kiro
 */
public class ResourceNotFoundException extends BusinessException {
    
    /**
     * 构造资源不存在异常
     * 
     * @param message 错误消息
     */
    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 构造资源不存在异常(带资源类型和ID)
     * 
     * @param resourceType 资源类型
     * @param resourceId 资源ID
     */
    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super("RESOURCE_NOT_FOUND", 
              String.format("%s(ID: %s)不存在", resourceType, resourceId), 
              HttpStatus.NOT_FOUND);
    }
}
