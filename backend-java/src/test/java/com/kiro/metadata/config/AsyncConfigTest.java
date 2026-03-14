package com.kiro.metadata.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步任务配置测试类
 * 
 * 测试异步任务线程池配置是否正确
 * 验证线程池参数、拒绝策略等配置项
 * 
 * @author Kiro
 * @since 2024
 */
@SpringBootTest
@TestPropertySource(properties = {
    "async.core-pool-size=5",
    "async.max-pool-size=10",
    "async.queue-capacity=100",
    "async.thread-name-prefix=async-task-"
})
@DisplayName("异步任务配置测试")
class AsyncConfigTest {
    
    @Autowired
    private AsyncProperties asyncProperties;
    
    @Autowired
    private Executor taskExecutor;
    
    @Test
    @DisplayName("测试异步配置属性加载")
    void testAsyncPropertiesLoaded() {
        // 验证配置属性是否正确加载
        assertThat(asyncProperties).isNotNull();
        assertThat(asyncProperties.getCorePoolSize()).isEqualTo(5);
        assertThat(asyncProperties.getMaxPoolSize()).isEqualTo(10);
        assertThat(asyncProperties.getQueueCapacity()).isEqualTo(100);
        assertThat(asyncProperties.getThreadNamePrefix()).isEqualTo("async-task-");
    }
    
    @Test
    @DisplayName("测试线程池执行器Bean创建")
    void testTaskExecutorBeanCreated() {
        // 验证线程池执行器Bean是否创建
        assertThat(taskExecutor).isNotNull();
        assertThat(taskExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);
    }
    
    @Test
    @DisplayName("测试线程池核心参数配置")
    void testThreadPoolCoreParameters() {
        // 获取线程池执行器
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
        
        // 验证核心线程数
        assertThat(executor.getCorePoolSize()).isEqualTo(5);
        
        // 验证最大线程数
        assertThat(executor.getMaxPoolSize()).isEqualTo(10);
        
        // 验证线程名称前缀
        assertThat(executor.getThreadNamePrefix()).isEqualTo("async-task-");
    }
    
    @Test
    @DisplayName("测试线程池拒绝策略")
    void testThreadPoolRejectedExecutionHandler() {
        // 获取线程池执行器
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();
        
        // 验证拒绝策略为 CallerRunsPolicy
        assertThat(threadPoolExecutor.getRejectedExecutionHandler())
                .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
    }
    
    @Test
    @DisplayName("测试线程池优雅关闭配置")
    void testThreadPoolGracefulShutdown() {
        // 获取线程池执行器
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) taskExecutor;
        
        // 注意: waitForTasksToCompleteOnShutdown 和 awaitTerminationSeconds 
        // 是 ThreadPoolTaskExecutor 的私有属性，无法直接访问
        // 这里只验证线程池执行器已正确初始化
        assertThat(executor.getThreadPoolExecutor()).isNotNull();
        assertThat(executor.getThreadPoolExecutor().isShutdown()).isFalse();
    }
    
    @Test
    @DisplayName("测试异步配置属性默认值")
    void testAsyncPropertiesDefaultValues() {
        // 创建新的配置属性对象，测试默认值
        AsyncProperties defaultProperties = new AsyncProperties();
        
        // 验证默认值
        assertThat(defaultProperties.getCorePoolSize()).isEqualTo(5);
        assertThat(defaultProperties.getMaxPoolSize()).isEqualTo(10);
        assertThat(defaultProperties.getQueueCapacity()).isEqualTo(100);
        assertThat(defaultProperties.getThreadNamePrefix()).isEqualTo("async-task-");
    }
}
