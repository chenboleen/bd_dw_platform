package com.kiro.metadata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 血缘图节点 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineageNode {
    
    /**
     * 表ID
     */
    private Long id;
    
    /**
     * 节点全名（数据库名.表名）
     */
    private String name;
    
    /**
     * 数据库名
     */
    private String databaseName;
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 表类型
     */
    private String tableType;
    
    /**
     * 深度（0为起点）
     */
    private Integer depth;
}
