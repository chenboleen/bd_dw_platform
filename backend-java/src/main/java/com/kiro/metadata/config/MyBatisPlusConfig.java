package com.kiro.metadata.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * 
 * @author Kiro
 * @version 1.0.0
 */
@Configuration
@MapperScan("com.kiro.metadata.repository")
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器配置
     * 包含分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(1000L);  // 单页最大记录数
        paginationInterceptor.setOverflow(false);  // 溢出总页数后是否进行处理
        
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        return interceptor;
    }
}
