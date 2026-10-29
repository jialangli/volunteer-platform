package com.gdupt.volunteer.attendance.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SigninRequest {

    /** 签到方式：QR（扫码）/GEO（定位）/MANUAL（组织者代签），默认 QR */
    @Pattern(regexp = "QR|GEO|MANUAL", message = "签到方式仅支持 QR/GEO/MANUAL")
    private String method;

    /** 代签目标用户ID：仅组织者/管理员可为他人代签（MANUAL） */
    private Long userId;
}
