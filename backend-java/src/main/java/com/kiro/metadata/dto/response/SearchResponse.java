package com.kiro.metadata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {
    
    /**
     * 表搜索结果
     */
    private List<TableResponse> tables;
    
    /**
     * 字段搜索结果
     */
    private List<ColumnResponse> columns;
    
    /**
     * 总结果数
     */
    private Long totalCount;
    
    /**
     * 搜索耗时(毫秒)
     */
    private Long searchTimeMs;
}
