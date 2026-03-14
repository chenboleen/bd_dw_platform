package com.kiro.metadata.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 目录响应 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogResponse {
    
    /**
     * 目录ID
     */
    private Long id;
    
    /**
     * 目录名称
     */
    private String name;
    
    /**
     * 目录描述
     */
    private String description;
    
    /**
     * 父目录ID
     */
    private Long parentId;
    
    /**
     * 层级
     */
    private Integer level;
    
    /**
     * 路径
     */
    private String path;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 创建人ID
     */
    private Long createdBy;
}
