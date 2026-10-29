package com.gdupt.volunteer.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserStatusRequest {

    /** 1 正常 / 0 禁用 */
    @NotNull(message = "状态不能为空")
    private Integer status;
}
