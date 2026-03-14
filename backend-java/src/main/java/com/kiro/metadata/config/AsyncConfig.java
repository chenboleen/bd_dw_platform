package com.kiro.metadata.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置类
 * 
 * 配置异步任务执行器和线程池，支持异步导出等耗时操作
 * 
 * 线程池工作原理:
 * 1. 任务提交时，如果线程数 < corePoolSize，创建新线程执行任务
 * 2. 如果线程数 >= corePoolSize，任务放入队列等待
 * 3. 如果队列已满且线程数 < maxPoolSize，创建新线程执行任务
 * 4. 如果队列已满且线程数 >= maxPoolSize，执行拒绝策略
 * 
 * @author Kiro
 * @since 2024
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {
    
    private final AsyncProperties asyncProperties;
    
    /**
     * 配置异步任务执行器
     * 
     * 线程池配置说明:
     * - 核心线程数: 线程池维护的最小线程数量，即使空闲也不会被回收
     * - 最大线程数: 线程池允许创建的最大线程数量
     * - 队列容量: 任务队列的最大容量，用于缓冲待执行的任务
     * - 线程名称前缀: 便于日志追踪和问题排查
     * - 拒绝策略: CallerRunsPolicy - 当线程池和队列都满时，由调用者线程执行任务
     * - 等待任务完成: 关闭线程池时等待所有任务执行完成，确保数据完整性
     * 
     * @return 配置好的线程池任务执行器
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        log.info("初始化异步任务线程池 - 核心线程数: {}, 最大线程数: {}, 队列容量: {}, 线程名称前缀: {}",
                asyncProperties.getCorePoolSize(),
                asyncProperties.getMaxPoolSize(),
                asyncProperties.getQueueCapacity(),
                asyncProperties.getThreadNamePrefix());
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(asyncProperties.getCorePoolSize());
        
        // 最大线程数
        executor.setMaxPoolSize(asyncProperties.getMaxPoolSize());
        
        // 队列容量
        executor.setQueueCapacity(asyncProperties.getQueueCapacity());
        
        // 线程名称前缀
        executor.setThreadNamePrefix(asyncProperties.getThreadNamePrefix());
        
        // 拒绝策略: CallerRunsPolicy
        // 当线程池和队列都满时，由调用者所在的线程来执行任务
        // 这样可以降低任务提交的速度，避免系统过载，同时保证任务不会丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待任务完成后再关闭线程池
        // 确保应用关闭时所有异步任务都能执行完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间: 60秒
        // 如果超过60秒任务还未完成，则强制关闭
        executor.setAwaitTerminationSeconds(60);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("异步任务线程池初始化完成");
        
        return executor;
    }
    
    /**
     * 配置异步任务异常处理器
     * 
     * 当异步方法抛出未捕获的异常时，此处理器会被调用
     * 记录详细的错误日志，便于问题排查
     * 
     * @return 异步异常处理器
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("异步任务执行异常 - 方法: {}.{}, 参数: {}, 异常信息: {}",
                    method.getDeclaringClass().getName(),
                    method.getName(),
                    params,
                    throwable.getMessage(),
                    throwable);
        };
    }
}
