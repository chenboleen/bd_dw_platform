package com.kiro.metadata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 异步任务配置属性
 * 
 * 从 application.yml 中读取 async.* 配置项
 * 用于配置异步任务线程池参数
 * 
 * @author Kiro
 * @since 2024
 */
@Data
@Component
@ConfigurationProperties(prefix = "async")
public class AsyncProperties {
    
    /**
     * 核心线程数
     * 线程池维护的最小线程数量
     * 默认值: 5
     */
    private int corePoolSize = 5;
    
    /**
     * 最大线程数
     * 线程池允许创建的最大线程数量
     * 默认值: 10
     */
    private int maxPoolSize = 10;
    
    /**
     * 队列容量
     * 任务队列的最大容量
     * 默认值: 100
     */
    private int queueCapacity = 100;
    
    /**
     * 线程名称前缀
     * 用于标识异步任务线程，便于日志追踪和问题排查
     * 默认值: async-task-
     */
    private String threadNamePrefix = "async-task-";
}
