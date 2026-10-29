package com.gdupt.volunteer.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRolesRequest {

    @NotEmpty(message = "角色ID列表不能为空")
    private List<Long> roleIds;
}
