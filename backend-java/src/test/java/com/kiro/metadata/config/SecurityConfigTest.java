package com.kiro.metadata.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Security 配置测试
 * 
 * 测试内容:
 * 1. SecurityConfig Bean 正确加载
 * 2. PasswordEncoder Bean 正确配置
 * 3. BCrypt 密码编码功能正常
 * 4. JWT 配置属性正确加载
 * 
 * @author Kiro
 * @since 2024-01-01
 */
@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=test-secret-key-for-unit-testing-minimum-256-bits",
    "jwt.expiration=3600000",
    "jwt.refresh-expiration=7200000"
})
class SecurityConfigTest {

    @Autowired(required = false)
    private SecurityConfig securityConfig;

    @Autowired(required = false)
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private JwtProperties jwtProperties;

    @Test
    void testSecurityConfigBeanLoaded() {
        // 验证: SecurityConfig Bean 应该被正确加载
        assertThat(securityConfig)
            .as("SecurityConfig Bean 应该存在")
            .isNotNull();
    }

    @Test
    void testPasswordEncoderBeanLoaded() {
        // 验证: PasswordEncoder Bean 应该被正确加载
        assertThat(passwordEncoder)
            .as("PasswordEncoder Bean 应该存在")
            .isNotNull();
    }

    @Test
    void testPasswordEncoderEncryption() {
        // 准备: 原始密码
        String rawPassword = "test123456";

        // 执行: 加密密码
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 验证: 加密后的密码应该不等于原始密码
        assertThat(encodedPassword)
            .as("加密后的密码不应该等于原始密码")
            .isNotEqualTo(rawPassword);

        // 验证: 加密后的密码应该以 $2a$ 或 $2b$ 开头 (BCrypt 格式)
        assertThat(encodedPassword)
            .as("BCrypt 密码应该以 $2a$ 或 $2b$ 开头")
            .matches("^\\$2[ab]\\$.*");
    }

    @Test
    void testPasswordEncoderMatching() {
        // 准备: 原始密码和加密密码
        String rawPassword = "test123456";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 验证: 正确的密码应该匹配
        assertThat(passwordEncoder.matches(rawPassword, encodedPassword))
            .as("正确的密码应该匹配成功")
            .isTrue();

        // 验证: 错误的密码不应该匹配
        assertThat(passwordEncoder.matches("wrongPassword", encodedPassword))
            .as("错误的密码不应该匹配")
            .isFalse();
    }

    @Test
    void testPasswordEncoderGeneratesDifferentHashes() {
        // 准备: 相同的原始密码
        String rawPassword = "test123456";

        // 执行: 两次加密
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        // 验证: 每次加密应该生成不同的哈希值 (因为使用了随机盐)
        assertThat(encoded1)
            .as("BCrypt 每次加密应该生成不同的哈希值")
            .isNotEqualTo(encoded2);

        // 验证: 但两个哈希值都应该能匹配原始密码
        assertThat(passwordEncoder.matches(rawPassword, encoded1))
            .as("第一个哈希值应该匹配原始密码")
            .isTrue();
        
        assertThat(passwordEncoder.matches(rawPassword, encoded2))
            .as("第二个哈希值应该匹配原始密码")
            .isTrue();
    }

    @Test
    void testJwtPropertiesLoaded() {
        // 验证: JwtProperties Bean 应该被正确加载
        assertThat(jwtProperties)
            .as("JwtProperties Bean 应该存在")
            .isNotNull();

        // 验证: JWT 密钥应该被正确加载
        assertThat(jwtProperties.getSecret())
            .as("JWT 密钥应该被正确加载")
            .isNotNull()
            .isNotEmpty();

        // 验证: JWT 过期时间应该被正确加载
        assertThat(jwtProperties.getExpiration())
            .as("JWT 访问令牌过期时间应该被正确加载")
            .isNotNull()
            .isPositive();

        assertThat(jwtProperties.getRefreshExpiration())
            .as("JWT 刷新令牌过期时间应该被正确加载")
            .isNotNull()
            .isPositive();
    }

    @Test
    void testJwtPropertiesValues() {
        // 验证: JWT 配置值应该符合预期
        assertThat(jwtProperties.getSecret())
            .as("JWT 密钥应该是测试密钥")
            .isEqualTo("test-secret-key-for-unit-testing-minimum-256-bits");

        assertThat(jwtProperties.getExpiration())
            .as("JWT 访问令牌过期时间应该是 1 小时")
            .isEqualTo(3600000L);

        assertThat(jwtProperties.getRefreshExpiration())
            .as("JWT 刷新令牌过期时间应该是 2 小时")
            .isEqualTo(7200000L);
    }

    @Test
    void testJwtSecretMinimumLength() {
        // 验证: JWT 密钥长度应该足够长 (建议 >= 32 字符)
        assertThat(jwtProperties.getSecret().length())
            .as("JWT 密钥长度应该 >= 32 字符以确保安全性")
            .isGreaterThanOrEqualTo(32);
    }

    @Test
    void testRefreshTokenExpirationLongerThanAccessToken() {
        // 验证: 刷新令牌过期时间应该大于访问令牌过期时间
        assertThat(jwtProperties.getRefreshExpiration())
            .as("刷新令牌过期时间应该大于访问令牌过期时间")
            .isGreaterThan(jwtProperties.getExpiration());
    }
}
