package com.kiro.metadata.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OpenAPI 配置测试类
 * 
 * 测试目标:
 * 1. 验证 OpenAPI Bean 正确创建
 * 2. 验证 API 基本信息配置
 * 3. 验证 JWT 认证方案配置
 * 4. 验证 API 标签和分组配置
 * 5. 验证服务器信息配置
 * 
 * @author Kiro
 * @since 2024-01-01
 */
@SpringBootTest
@DisplayName("OpenAPI 配置测试")
class OpenApiConfigTest {

    @Autowired
    private OpenAPI openAPI;

    @Test
    @DisplayName("应该成功创建 OpenAPI Bean")
    void shouldCreateOpenAPIBean() {
        // 验证 OpenAPI Bean 不为空
        assertThat(openAPI).isNotNull();
    }

    @Test
    @DisplayName("应该正确配置 API 基本信息")
    void shouldConfigureApiInfo() {
        // 获取 API 信息
        Info info = openAPI.getInfo();
        
        // 验证 API 信息不为空
        assertThat(info).isNotNull();
        
        // 验证标题
        assertThat(info.getTitle())
            .isEqualTo("数据仓库元数据管理系统 API");
        
        // 验证版本
        assertThat(info.getVersion())
            .isEqualTo("1.0.0");
        
        // 验证描述包含关键信息
        assertThat(info.getDescription())
            .contains("企业级数据仓库元数据管理平台")
            .contains("RESTful API 文档")
            .contains("功能特性")
            .contains("认证方式")
            .contains("响应格式")
            .contains("错误码说明");
        
        // 验证联系信息
        assertThat(info.getContact()).isNotNull();
        assertThat(info.getContact().getName()).isEqualTo("Kiro Team");
        assertThat(info.getContact().getEmail()).isEqualTo("support@kiro.com");
        assertThat(info.getContact().getUrl()).isEqualTo("https://www.kiro.com");
        
        // 验证许可证信息
        assertThat(info.getLicense()).isNotNull();
        assertThat(info.getLicense().getName()).isEqualTo("Apache 2.0");
        assertThat(info.getLicense().getUrl())
            .isEqualTo("https://www.apache.org/licenses/LICENSE-2.0.html");
    }

    @Test
    @DisplayName("应该正确配置 JWT 认证方案")
    void shouldConfigureJwtSecurity() {
        // 验证安全要求
        List<SecurityRequirement> securityRequirements = openAPI.getSecurity();
        assertThat(securityRequirements).isNotEmpty();
        
        SecurityRequirement securityRequirement = securityRequirements.get(0);
        assertThat(securityRequirement.containsKey("Bearer认证")).isTrue();
        
        // 验证安全方案
        SecurityScheme securityScheme = openAPI.getComponents()
            .getSecuritySchemes()
            .get("Bearer认证");
        
        assertThat(securityScheme).isNotNull();
        assertThat(securityScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(securityScheme.getScheme()).isEqualTo("bearer");
        assertThat(securityScheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(securityScheme.getIn()).isEqualTo(SecurityScheme.In.HEADER);
        assertThat(securityScheme.getName()).isEqualTo("Authorization");
        
        // 验证描述包含使用说明
        assertThat(securityScheme.getDescription())
            .contains("JWT 认证方案")
            .contains("Bearer {token}")
            .contains("24 小时");
    }

    @Test
    @DisplayName("应该正确配置 API 标签")
    void shouldConfigureApiTags() {
        // 获取标签列表
        List<Tag> tags = openAPI.getTags();
        
        // 验证标签不为空
        assertThat(tags).isNotEmpty();
        
        // 验证标签数量（应该有 10 个标签）
        assertThat(tags).hasSize(10);
        
        // 验证关键标签存在
        List<String> tagNames = tags.stream()
            .map(Tag::getName)
            .toList();
        
        assertThat(tagNames).contains(
            "认证管理",
            "表元数据",
            "字段元数据",
            "血缘关系",
            "搜索功能",
            "数据目录",
            "变更历史",
            "数据质量",
            "导入导出",
            "系统监控"
        );
        
        // 验证标签描述
        Tag authTag = tags.stream()
            .filter(tag -> "认证管理".equals(tag.getName()))
            .findFirst()
            .orElse(null);
        
        assertThat(authTag).isNotNull();
        assertThat(authTag.getDescription()).isEqualTo("用户认证和授权相关接口");
    }

    @Test
    @DisplayName("应该正确配置服务器信息")
    void shouldConfigureServers() {
        // 获取服务器列表
        List<Server> servers = openAPI.getServers();
        
        // 验证服务器不为空
        assertThat(servers).isNotEmpty();
        
        // 验证至少有 2 个服务器（本地开发和生产环境）
        assertThat(servers).hasSizeGreaterThanOrEqualTo(2);
        
        // 验证本地开发服务器
        Server localServer = servers.stream()
            .filter(server -> server.getUrl().contains("localhost"))
            .findFirst()
            .orElse(null);
        
        assertThat(localServer).isNotNull();
        assertThat(localServer.getUrl()).isEqualTo("http://localhost:8080");
        assertThat(localServer.getDescription()).isEqualTo("本地开发环境");
    }

    @Test
    @DisplayName("应该配置外部文档链接")
    void shouldConfigureExternalDocs() {
        // 验证外部文档
        assertThat(openAPI.getExternalDocs()).isNotNull();
        assertThat(openAPI.getExternalDocs().getDescription())
            .isEqualTo("数据仓库元数据管理系统完整文档");
        assertThat(openAPI.getExternalDocs().getUrl())
            .isEqualTo("https://docs.kiro.com/metadata-management");
    }

    @Test
    @DisplayName("应该配置组件")
    void shouldConfigureComponents() {
        // 验证组件不为空
        assertThat(openAPI.getComponents()).isNotNull();
        
        // 验证安全方案已配置
        assertThat(openAPI.getComponents().getSecuritySchemes())
            .isNotEmpty()
            .containsKey("Bearer认证");
    }
}
