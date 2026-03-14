package com.kiro.metadata.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SQL解析请求 DTO
 * 用于从SQL语句中提取血缘关系
 * 
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SqlParseRequest {
    
    /**
     * 待解析的SQL语句
     * 支持 SELECT、INSERT INTO...SELECT、CREATE TABLE AS SELECT 等类型
     */
    @NotBlank(message = "SQL语句不能为空")
    private String sql;
}
