package com.kiro.metadata.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 请求日志拦截器
 * 记录每个请求的详细信息，包括请求ID、用户、路径、耗时等
 * 对应需求: 7.5, 13.4（API 日志记录）
 */
@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER_ID = "userId";
    private static final String ATTR_START_TIME = "requestStartTime";
    private static final String HEADER_PROCESS_TIME = "X-Process-Time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        MDC.put(MDC_REQUEST_ID, requestId);
        String userId = getCurrentUserId();
        MDC.put(MDC_USER_ID, userId);
        request.setAttribute(ATTR_START_TIME, System.currentTimeMillis());
        String method = request.getMethod();
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        log.info("收到请求 - requestId: {}, 用户: {}, 方法: {}, 路径: {}, 查询参数: {}, 客户端IP: {}, UserAgent: {}",
                requestId, userId, method, path,
                queryString != null ? queryString : "",
                clientIp,
                userAgent != null ? userAgent : "");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception exception) {
        try {
            Long startTime = (Long) request.getAttribute(ATTR_START_TIME);
            long processTime = startTime != null ? System.currentTimeMillis() - startTime : 0L;
            response.setHeader(HEADER_PROCESS_TIME, processTime + "ms");
            int status = response.getStatus();
            String requestId = MDC.get(MDC_REQUEST_ID);
            String path = request.getRequestURI();
            if (exception != null) {
                log.error("请求处理异常 - requestId: {}, 路径: {}, 状态码: {}, 耗时: {}ms, 异常: {}",
                        requestId, path, status, processTime, exception.getMessage());
            } else if (status >= 500) {
                log.error("请求处理失败 - requestId: {}, 路径: {}, 状态码: {}, 耗时: {}ms",
                        requestId, path, status, processTime);
            } else if (status >= 400) {
                log.warn("请求处理警告 - requestId: {}, 路径: {}, 状态码: {}, 耗时: {}ms",
                        requestId, path, status, processTime);
            } else {
                log.info("请求处理完成 - requestId: {}, 路径: {}, 状态码: {}, 耗时: {}ms",
                        requestId, path, status, processTime);
            }
        } finally {
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("获取当前用户信息失败: {}", e.getMessage());
        }
        return "anonymous";
    }

    private String getClientIp(HttpServletRequest request) {
        String[] ipHeaders = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP"};
        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
