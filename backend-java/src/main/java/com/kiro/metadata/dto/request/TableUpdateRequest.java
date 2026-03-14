package com.kiro.metadata.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 表更新请求 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableUpdateRequest {
    
    /**
     * 表描述
     */
    private String description;
    
    /**
     * 存储格式
     */
    private String storageFormat;
    
    /**
     * 存储位置
     */
    private String storageLocation;
    
    /**
     * 数据大小(字节)
     */
    private Long dataSizeBytes;
}
