package com.kiro.metadata.util;

import com.kiro.metadata.document.TableDocument;
import com.kiro.metadata.entity.TableMetadata;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表元数据与 Elasticsearch 文档转换工具类
 * 注意: 由于使用 MyBatis-Plus,关联数据需要在服务层单独查询后传入
 */
@UtilityClass
public class TableDocumentMapper {

    /**
     * 将表元数据实体转换为 Elasticsearch 文档
     * 
     * @param table 表元数据实体
     * @return Elasticsearch 文档
     */
    public static TableDocument toDocument(TableMetadata table) {
        if (table == null) {
            return null;
        }

        // 注意: 字段名、目录路径等关联数据需要在服务层查询后通过重载方法传入
        return toDocument(table, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null, null);
    }
    
    /**
     * 将表元数据实体转换为 Elasticsearch 文档(包含关联数据)
     * 
     * @param table 表元数据实体
     * @param columnNames 字段名列表
     * @param columnDescriptions 字段描述列表
     * @param catalogPaths 目录路径列表
     * @param ownerUsername 所有者用户名
     * @param ownerEmail 所有者邮箱
     * @return Elasticsearch 文档
     */
    public static TableDocument toDocument(TableMetadata table, 
                                          List<String> columnNames,
                                          List<String> columnDescriptions,
                                          List<String> catalogPaths,
                                          String ownerUsername,
                                          String ownerEmail) {
        if (table == null) {
            return null;
        }

        // 构建文档
        return TableDocument.builder()
                .id(table.getId().toString())
                .databaseName(table.getDatabaseName())
                .tableName(table.getTableName())
                .tableType(table.getTableType() != null ? table.getTableType().name() : null)
                .description(table.getDescription())
                .storageFormat(table.getStorageFormat())
                .storageLocation(table.getStorageLocation())
                .dataSizeBytes(table.getDataSizeBytes())
                .ownerUsername(ownerUsername)
                .ownerEmail(ownerEmail)
                .columnNames(columnNames != null ? columnNames : new ArrayList<>())
                .columnDescriptions(columnDescriptions != null ? columnDescriptions : new ArrayList<>())
                .tags(new ArrayList<>())  // 标签功能待实现
                .catalogPaths(catalogPaths != null ? catalogPaths : new ArrayList<>())
                .createdAt(table.getCreatedAt())
                .updatedAt(table.getUpdatedAt())
                .lastAccessedAt(table.getLastAccessedAt())
                .recordCount(null)  // 从质量指标获取
                .nullRate(null)     // 从质量指标获取
                .dataFreshnessHours(null)  // 从质量指标获取
                .isActive(true)
                .searchScore(1.0f)
                .build();
    }

    /**
     * 批量转换表元数据实体为 Elasticsearch 文档
     * 
     * @param tables 表元数据实体列表
     * @return Elasticsearch 文档列表
     */
    public static List<TableDocument> toDocuments(List<TableMetadata> tables) {
        if (tables == null || tables.isEmpty()) {
            return new ArrayList<>();
        }

        return tables.stream()
                .map(TableDocumentMapper::toDocument)
                .collect(Collectors.toList());
    }
}
