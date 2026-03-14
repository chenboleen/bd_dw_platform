package com.kiro.metadata.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 表元数据 Elasticsearch 文档 POJO
 * 不使用 spring-data-elasticsearch 注解，直接作为 JSON 序列化对象
 * 与 co.elastic.clients Java 客户端配合使用
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDocument {

    /** 表 ID */
    private String id;

    /** 数据库名 */
    private String databaseName;

    /** 表名 */
    private String tableName;

    /** 表类型（TABLE, VIEW, EXTERNAL） */
    private String tableType;

    /** 表描述 */
    private String description;

    /** 存储格式 */
    private String storageFormat;

    /** 存储位置 */
    private String storageLocation;

    /** 数据大小（字节） */
    private Long dataSizeBytes;

    /** 所有者用户名 */
    private String ownerUsername;

    /** 所有者邮箱 */
    private String ownerEmail;

    /** 字段名列表 */
    private List<String> columnNames;

    /** 字段描述列表 */
    private List<String> columnDescriptions;

    /** 标签列表 */
    private List<String> tags;

    /** 目录路径列表 */
    private List<String> catalogPaths;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 最后访问时间 */
    private LocalDateTime lastAccessedAt;

    /** 记录数 */
    private Long recordCount;

    /** 空值率 */
    private Double nullRate;

    /** 数据新鲜度（小时） */
    private Integer dataFreshnessHours;

    /** 是否活跃 */
    private Boolean isActive;

    /** 搜索评分 */
    private Float searchScore;
}
