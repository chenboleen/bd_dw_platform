package com.kiro.metadata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 血缘图 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineageGraph {
    
    /**
     * 节点列表
     */
    private List<LineageNode> nodes;
    
    /**
     * 边列表
     */
    private List<LineageEdge> edges;
    
    /**
     * 最大层级
     */
    private Integer maxLevel;
}
