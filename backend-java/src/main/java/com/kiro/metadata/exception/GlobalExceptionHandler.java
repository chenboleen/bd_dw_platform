package com.kiro.metadata.exception;

import com.kiro.metadata.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理应用中的各种异常,返回标准的错误响应格式
 * 
 * @author Kiro
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    /**
     * 处理资源不存在异常
     * 
     * @param ex 资源不存在异常
     * @param request HTTP 请求
     * @return 404 错误响应
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, 
            HttpServletRequest request) {
        log.error("资源不存在: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .details(formatDetails(ex.getDetails()))
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    /**
     * 处理权限不足异常
     * 
     * @param ex 权限不足异常
     * @param request HTTP 请求
     * @return 403 错误响应
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex, 
            HttpServletRequest request) {
        log.error("权限不足: {}", ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .details(formatDetails(ex.getDetails()))
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }
    
    /**
     * 处理业务异常
     * 
     * @param ex 业务异常
     * @param request HTTP 请求
     * @return 根据异常类型返回相应的状态码
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, 
            HttpServletRequest request) {
        log.error("业务异常: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ErrorResponse error = ErrorResponse.builder()
            .code(ex.getErrorCode())
            .message(ex.getMessage())
            .details(formatDetails(ex.getDetails()))
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }
    
    /**
     * 处理数据验证异常
     * 
     * @param ex 方法参数验证异常
     * @param request HTTP 请求
     * @return 422 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        log.error("数据验证失败: {}", ex.getMessage());
        
        // 收集所有字段验证错误
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                fieldErrors.put(error.getObjectName(), error.getDefaultMessage());
            }
        });
        
        String detailsMessage = fieldErrors.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining("; "));
        
        ErrorResponse error = ErrorResponse.builder()
            .code("VALIDATION_ERROR")
            .message("数据验证失败")
            .details(detailsMessage)
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }
    
    /**
     * 处理数据库访问异常
     * 
     * @param ex 数据访问异常
     * @param request HTTP 请求
     * @return 503 错误响应
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException ex,
            HttpServletRequest request) {
        log.error("数据库访问错误: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("DATABASE_ERROR")
            .message("数据库服务暂时不可用,请稍后重试")
            .details("数据库操作失败")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }
    
    /**
     * 处理 Spring MVC 路由不存在异常（404）
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        log.warn("请求路径不存在: {}", request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
            .code("NOT_FOUND")
            .message("请求的接口路径不存在: " + request.getRequestURI())
            .details(ex.getMessage())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 处理所有未捕获的异常
     * 
     * @param ex 异常
     * @param request HTTP 请求
     * @return 500 错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        log.error("未处理的异常: {}", ex.getMessage(), ex);
        
        ErrorResponse error = ErrorResponse.builder()
            .code("INTERNAL_ERROR")
            .message("服务器内部错误,请联系管理员")
            .details(ex.getClass().getSimpleName())
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    /**
     * 格式化详细信息为字符串
     * 
     * @param details 详细信息 Map
     * @return 格式化后的字符串
     */
    private String formatDetails(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        
        return details.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining(", "));
    }
}
