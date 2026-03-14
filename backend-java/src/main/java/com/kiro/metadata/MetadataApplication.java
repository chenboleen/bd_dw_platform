package com.kiro.metadata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据仓库元数据管理系统 - 主应用类
 *
 * 使用纯 Elasticsearch Java 客户端（co.elastic.clients），
 * 不引入 spring-data-elasticsearch，彻底避免 Spring Boot 3.2.x
 * 的 factoryBeanObjectType 类型不匹配问题。
 *
 * @author Kiro
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class MetadataApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetadataApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("数据仓库元数据管理系统启动成功!");
        System.out.println("Swagger 文档地址: http://localhost:8080/swagger-ui.html");
        System.out.println("========================================\n");
    }
}
