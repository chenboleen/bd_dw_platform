package com.kiro.metadata.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kiro.metadata.dto.response.ErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * JWT 访问拒绝处理器
 * 处理已认证用户访问无权限资源的情况
 * 
 * @author Kiro
 */
@Component
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    
    private final ObjectMapper objectMapper;
    
    public JwtAccessDeniedHandler() {
        this.objectMapper = new ObjectMapper();
        // 注册 JavaTimeModule 以支持 LocalDateTime 序列化
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    /**
     * 处理访问被拒绝的请求
     * 当用户已认证但权限不足时,返回 403 错误响应
     * 
     * @param request HTTP 请求
     * @param response HTTP 响应
     * @param accessDeniedException 访问拒绝异常
     * @throws IOException IO 异常
     * @throws ServletException Servlet 异常
     */
    @Override
    public void handle(HttpServletRequest request,
                      HttpServletResponse response,
                      AccessDeniedException accessDeniedException) 
            throws IOException, ServletException {
        
        // 获取当前认证用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication != null ? authentication.getName() : "未知用户";
        
        // 记录权限不足日志
        log.warn("权限不足 - 用户: {}, 请求路径: {}, 错误信息: {}, IP: {}", 
                username,
                request.getRequestURI(), 
                accessDeniedException.getMessage(),
                getClientIp(request));
        
        // 构建错误响应
        ErrorResponse errorResponse = ErrorResponse.builder()
                .code("FORBIDDEN")
                .message("权限不足,无法访问该资源")
                .details(String.format("用户 '%s' 没有权限执行此操作", username))
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .build();
        
        // 设置响应状态码和内容类型
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        
        // 写入 JSON 响应
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
    
    /**
     * 获取客户端真实 IP 地址
     * 考虑代理和负载均衡的情况
     * 
     * @param request HTTP 请求
     * @return 客户端 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果有多个代理,取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
