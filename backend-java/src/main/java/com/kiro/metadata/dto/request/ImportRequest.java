package com.kiro.metadata.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 导入请求 DTO
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportRequest {
    
    /**
     * 文件内容(Base64编码或JSON字符串)
     */
    @NotBlank(message = "文件内容不能为空")
    private String fileContent;
    
    /**
     * 文件类型(CSV/JSON)
     */
    @NotBlank(message = "文件类型不能为空")
    private String fileType;
    
    /**
     * 是否覆盖已存在的数据
     */
    private Boolean overwrite = false;
}
