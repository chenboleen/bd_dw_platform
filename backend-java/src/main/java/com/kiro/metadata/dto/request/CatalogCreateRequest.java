package com.kiro.metadata.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

/**
 * 目录创建请求 DTO
 * level、path 由后端根据 parentId 自动计算，createdBy 从登录用户获取
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
     * 父目录ID（为空则创建根目录）
     */
    private Long parentId;
}
