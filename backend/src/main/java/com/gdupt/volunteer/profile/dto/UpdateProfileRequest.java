package com.gdupt.volunteer.profile.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(max = 50, message = "姓名长度不能超过50")
    private String realName;

    @Size(max = 20, message = "手机号长度不能超过20")
    private String phone;

    @Size(max = 30, message = "学号长度不能超过30")
    private String studentNo;
}
