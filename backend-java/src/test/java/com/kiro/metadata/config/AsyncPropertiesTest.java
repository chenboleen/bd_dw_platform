package com.kiro.metadata.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步配置属性测试类
 * 
 * 测试 AsyncProperties 的默认值和基本功能
 * 不依赖 Spring 容器，直接测试 POJO 类
 * 
 * @author Kiro
 * @since 2024
 */
@DisplayName("异步配置属性测试")
class AsyncPropertiesTest {
    
    @Test
    @DisplayName("测试默认值配置")
    void testDefaultValues() {
        // 创建配置属性对象
        AsyncProperties properties = new AsyncProperties();
        
        // 验证默认值
        assertThat(properties.getCorePoolSize()).isEqualTo(5);
        assertThat(properties.getMaxPoolSize()).isEqualTo(10);
        assertThat(properties.getQueueCapacity()).isEqualTo(100);
        assertThat(properties.getThreadNamePrefix()).isEqualTo("async-task-");
    }
    
    @Test
    @DisplayName("测试属性设置和获取")
    void testSettersAndGetters() {
        // 创建配置属性对象
        AsyncProperties properties = new AsyncProperties();
        
        // 设置自定义值
        properties.setCorePoolSize(8);
        properties.setMaxPoolSize(16);
        properties.setQueueCapacity(200);
        properties.setThreadNamePrefix("custom-task-");
        
        // 验证设置的值
        assertThat(properties.getCorePoolSize()).isEqualTo(8);
        assertThat(properties.getMaxPoolSize()).isEqualTo(16);
        assertThat(properties.getQueueCapacity()).isEqualTo(200);
        assertThat(properties.getThreadNamePrefix()).isEqualTo("custom-task-");
    }
    
    @Test
    @DisplayName("测试配置属性对象创建")
    void testObjectCreation() {
        // 验证对象可以正常创建
        AsyncProperties properties = new AsyncProperties();
        assertThat(properties).isNotNull();
    }
}
