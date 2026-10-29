package com.gdupt.volunteer.certification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class IssueCertRequest {

    @NotNull(message = "志愿者ID不能为空")
    private Long userId;

    /** 证明周期，如 2025-2026 学年 */
    @NotBlank(message = "证明周期不能为空")
    @Size(max = 30, message = "证明周期长度不能超过30")
    private String periodLabel;
}
