package com.kiro.metadata.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kiro.metadata.entity.TableMetadata;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 表元数据 Repository
 * 
 * @author Kiro
 */
@Mapper
public interface TableRepository extends BaseMapper<TableMetadata> {
    
    /**
     * 根据数据库名和表名查询表
     * 
     * @param databaseName 数据库名
     * @param tableName 表名
     * @return 表元数据
     */
    default Optional<TableMetadata> findByDatabaseAndTable(String databaseName, String tableName) {
        return Optional.ofNullable(
            selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TableMetadata>()
                    .eq(TableMetadata::getDatabaseName, databaseName)
                    .eq(TableMetadata::getTableName, tableName)
            )
        );
    }
    
    /**
     * 根据数据库名查询所有表
     * 
     * @param databaseName 数据库名
     * @return 表列表
     */
    default List<TableMetadata> findByDatabaseName(String databaseName) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TableMetadata>()
                .eq(TableMetadata::getDatabaseName, databaseName)
        );
    }
    
    /**
     * 根据所有者ID查询表
     * 
     * @param ownerId 所有者ID
     * @return 表列表
     */
    default List<TableMetadata> findByOwnerId(Long ownerId) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TableMetadata>()
                .eq(TableMetadata::getOwnerId, ownerId)
        );
    }
}
