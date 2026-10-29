package com.gdupt.volunteer.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TypeRequest {

    @NotBlank(message = "类型名称不能为空")
    @Size(max = 50, message = "类型名称长度不能超过50")
    private String name;

    private Integer sortOrder;
}
