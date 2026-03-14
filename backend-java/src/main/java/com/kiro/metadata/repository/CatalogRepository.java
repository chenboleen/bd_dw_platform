package com.kiro.metadata.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kiro.metadata.entity.Catalog;
import com.kiro.metadata.entity.TableMetadata;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 数据目录 Repository
 * 
 * @author Kiro
 */
@Mapper
public interface CatalogRepository extends BaseMapper<Catalog> {
    
    /**
     * 根据父目录ID查询子目录
     * 
     * @param parentId 父目录ID
     * @return 目录列表
     */
    default List<Catalog> findByParentId(Long parentId) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Catalog>()
                .eq(Catalog::getParentId, parentId)
        );
    }
    
    /**
     * 根据层级查询目录
     * 
     * @param level 层级
     * @return 目录列表
     */
    default List<Catalog> findByLevel(Integer level) {
        return selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Catalog>()
                .eq(Catalog::getLevel, level)
        );
    }
    
    /**
     * 根据路径查询目录
     * 
     * @param path 路径
     * @return 目录
     */
    default Catalog findByPath(String path) {
        return selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Catalog>()
                .eq(Catalog::getPath, path)
        );
    }
    
    /**
     * 将表添加到目录
     * 
     * @param tableId 表ID
     * @param catalogId 目录ID
     */
    @Insert("INSERT INTO table_catalog (table_id, catalog_id) VALUES (#{tableId}, #{catalogId})")
    void addTableToCatalog(@Param("tableId") Long tableId, @Param("catalogId") Long catalogId);
    
    /**
     * 从目录移除表
     * 
     * @param tableId 表ID
     * @param catalogId 目录ID
     */
    @Delete("DELETE FROM table_catalog WHERE table_id = #{tableId} AND catalog_id = #{catalogId}")
    void removeTableFromCatalog(@Param("tableId") Long tableId, @Param("catalogId") Long catalogId);
    
    /**
     * 获取目录下的表
     * 
     * @param catalogId 目录ID
     * @return 表列表
     */
    @Select("SELECT t.* FROM tables t " +
            "INNER JOIN table_catalog tc ON t.id = tc.table_id " +
            "WHERE tc.catalog_id = #{catalogId}")
    List<TableMetadata> getTablesInCatalog(@Param("catalogId") Long catalogId);

    /**
     * 根据表ID查询关联的数据域（取第一个）
     * 
     * @param tableId 表ID
     * @return 数据域
     */
    @Select("SELECT c.* FROM catalog c " +
            "INNER JOIN table_catalog tc ON c.id = tc.catalog_id " +
            "WHERE tc.table_id = #{tableId} LIMIT 1")
    Catalog findCatalogByTableId(@Param("tableId") Long tableId);
}
