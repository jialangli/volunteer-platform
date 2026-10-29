package com.gdupt.volunteer.activity.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuditRequest {

    /** true=通过 false=拒绝 */
    @NotNull(message = "审核结果不能为空")
    private Boolean approve;

    @Size(max = 200, message = "审核意见最长 200 字")
    private String reason;
}
