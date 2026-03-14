package com.kiro.metadata.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * OpenAPI (Swagger) 配置类
 * 
 * 功能说明:
 * 1. 配置 API 文档基本信息（标题、版本、描述）
 * 2. 配置 JWT Bearer Token 认证方案
 * 3. 配置全局安全要求
 * 4. 配置 API 标签和分组
 * 5. 配置服务器信息
 * 
 * 访问地址:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - API 文档: http://localhost:8080/v3/api-docs
 * 
 * @author Kiro
 * @since 2024-01-01
 */
@Configuration
public class OpenApiConfig {

    /**
     * 配置 OpenAPI 文档
     * 
     * @return OpenAPI 实例
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(apiInfo())
            .externalDocs(externalDocumentation())
            .servers(Arrays.asList(
                new Server().url("http://localhost:8080").description("本地开发环境"),
                new Server().url("https://api.kiro.com").description("生产环境")
            ))
            .addSecurityItem(securityRequirement())
            .components(components())
            .tags(Arrays.asList(
                new Tag().name("认证管理").description("用户认证和授权相关接口"),
                new Tag().name("表元数据").description("数据表元数据管理接口"),
                new Tag().name("字段元数据").description("表字段元数据管理接口"),
                new Tag().name("血缘关系").description("数据血缘关系分析接口"),
                new Tag().name("搜索功能").description("元数据全文搜索接口"),
                new Tag().name("数据目录").description("数据目录组织管理接口"),
                new Tag().name("变更历史").description("元数据变更历史追踪接口"),
                new Tag().name("数据质量").description("数据质量指标展示接口"),
                new Tag().name("导入导出").description("元数据批量导入导出接口"),
                new Tag().name("系统监控").description("系统健康检查和监控接口")
            ));
    }

    /**
     * API 基本信息
     * 
     * @return Info 实例
     */
    private Info apiInfo() {
        return new Info()
            .title("数据仓库元数据管理系统 API")
            .description("""
                企业级数据仓库元数据管理平台 RESTful API 文档
                
                ## 功能特性
                
                - **元数据管理**: 管理表和字段的元数据信息
                - **血缘分析**: 分析和可视化表之间的依赖关系
                - **全文搜索**: 提供高性能的元数据搜索功能
                - **数据目录**: 组织和分类元数据的层级结构
                - **权限控制**: 基于角色的访问控制（RBAC）
                - **数据质量**: 展示和监控数据质量指标
                - **变更追踪**: 追踪元数据变更历史记录
                - **批量操作**: 支持元数据的批量导入导出
                
                ## 认证方式
                
                本 API 使用 JWT (JSON Web Token) 进行身份认证。
                
                1. 调用 `/api/v1/auth/login` 接口获取 Token
                2. 在后续请求的 Header 中添加: `Authorization: Bearer {token}`
                3. Token 有效期为 24 小时
                
                ## 响应格式
                
                所有 API 响应均为 JSON 格式，包含以下字段：
                
                - `success`: 请求是否成功（boolean）
                - `data`: 响应数据（object）
                - `message`: 响应消息（string）
                - `timestamp`: 响应时间戳（long）
                
                ## 错误码说明
                
                - `200`: 请求成功
                - `400`: 请求参数错误
                - `401`: 未认证或 Token 无效
                - `403`: 无权限访问
                - `404`: 资源不存在
                - `500`: 服务器内部错误
                """)
            .version("1.0.0")
            .contact(new Contact()
                .name("Kiro Team")
                .email("support@kiro.com")
                .url("https://www.kiro.com"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html"));
    }

    /**
     * 外部文档链接
     * 
     * @return ExternalDocumentation 实例
     */
    private ExternalDocumentation externalDocumentation() {
        return new ExternalDocumentation()
            .description("数据仓库元数据管理系统完整文档")
            .url("https://docs.kiro.com/metadata-management");
    }

    /**
     * 安全要求配置
     * 
     * @return SecurityRequirement 实例
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("Bearer认证");
    }

    /**
     * 组件配置 (包含安全方案)
     * 
     * @return Components 实例
     */
    private Components components() {
        return new Components()
            .addSecuritySchemes("Bearer认证", securityScheme());
    }

    /**
     * JWT Bearer Token 安全方案
     * 
     * @return SecurityScheme 实例
     */
    private SecurityScheme securityScheme() {
        return new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .in(SecurityScheme.In.HEADER)
            .name("Authorization")
            .description("""
                JWT 认证方案
                
                请在此处输入从登录接口获取的 JWT Token。
                
                格式: Bearer {token}
                
                示例: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
                
                注意: 
                - Token 前需要添加 "Bearer " 前缀（注意空格）
                - Token 有效期为 24 小时
                - Token 过期后需要重新登录获取新的 Token
                """);
    }
}
