package com.kiro.metadata.security;

import com.kiro.metadata.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token 工具类
 * 
 * 负责生成、验证和解析 JWT Token
 * 
 * @author Kiro
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    
    private final JwtProperties jwtProperties;
    
    /**
     * 生成访问令牌
     * 
     * @param authentication 认证信息
     * @return JWT Token
     */
    public String generateAccessToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails.getUsername(), jwtProperties.getExpiration());
    }
    
    /**
     * 生成访问令牌(根据用户名)
     * 
     * @param username 用户名
     * @return JWT Token
     */
    public String generateAccessToken(String username) {
        return generateToken(username, jwtProperties.getExpiration());
    }
    
    /**
     * 生成刷新令牌
     * 
     * @param username 用户名
     * @return JWT Token
     */
    public String generateRefreshToken(String username) {
        return generateToken(username, jwtProperties.getRefreshExpiration());
    }
    
    /**
     * 生成 Token
     * 
     * @param username 用户名
     * @param expiration 过期时间(毫秒)
     * @return JWT Token
     */
    @SuppressWarnings("deprecation")
    private String generateToken(String username, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }
    
    /**
     * 从 Token 中获取用户名
     * 
     * @param token JWT Token
     * @return 用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getSubject();
    }
    
    /**
     * 验证 Token 是否有效
     * 
     * @param token JWT Token
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.error("无效的 JWT 签名");
        } catch (MalformedJwtException ex) {
            log.error("无效的 JWT Token");
        } catch (ExpiredJwtException ex) {
            log.error("JWT Token 已过期");
        } catch (UnsupportedJwtException ex) {
            log.error("不支持的 JWT Token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT Claims 字符串为空");
        }
        return false;
    }
    
    /**
     * 获取签名密钥
     * 
     * @return 签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 获取 Token 过期时间(秒)
     * 
     * @return 过期时间
     */
    public Long getExpirationInSeconds() {
        return jwtProperties.getExpiration() / 1000;
    }
}
