package com.kiro.metadata.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 目录创建请求 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogCreateRequest {
    
    /**
     * 目录名称
     */
    @NotBlank(message = "目录名称不能为空")
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
     * 层级(1-5)
     */
    @NotNull(message = "层级不能为空")
    @Min(value = 1, message = "层级必须在1-5之间")
    @Max(value = 5, message = "层级必须在1-5之间")
    private Integer level;
    
    /**
     * 路径
     */
    @NotBlank(message = "路径不能为空")
    private String path;
    
    /**
     * 创建人ID
     */
    @NotNull(message = "创建人ID不能为空")
    private Long createdBy;
}
