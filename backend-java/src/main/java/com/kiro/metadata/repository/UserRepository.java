package com.kiro.metadata.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kiro.metadata.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/**
 * 用户 Repository
 * 
 * @author Kiro
 */
@Mapper
public interface UserRepository extends BaseMapper<User> {
    
    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    default Optional<User> findByUsername(String username) {
        return Optional.ofNullable(
            selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                    .eq(User::getUsername, username)
            )
        );
    }
    
    /**
     * 根据邮箱查询用户
     * 
     * @param email 邮箱
     * @return 用户信息
     */
    default Optional<User> findByEmail(String email) {
        return Optional.ofNullable(
            selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email)
            )
        );
    }
}
