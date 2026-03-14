package com.kiro.metadata.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 字段排序请求 DTO
 * 用于批量调整表中字段的显示顺序
 *
 * @author Kiro
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReorderColumnsRequest {

    /**
     * 表ID
     */
    @NotNull(message = "表ID不能为空")
    private Long tableId;

    /**
     * 字段ID列表（按新顺序排列）
     * 列表中的顺序即为字段的新排列顺序
     */
    @NotEmpty(message = "字段ID列表不能为空")
    private List<Long> columnIds;
}
