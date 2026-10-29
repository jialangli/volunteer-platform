package com.gdupt.volunteer.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一错误码。
 * 2xx 为成功；4xx 为 HTTP 层错误；1xxx 起为业务错误码。
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    OK(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限执行该操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "操作冲突"),
    RATE_LIMITED(429, "操作过于频繁，请稍后再试"),
    INTERNAL_ERROR(500, "系统内部错误"),

    // 业务错误码 1xxx
    USERNAME_OR_PASSWORD_ERROR(1001, "用户名或密码错误"),
    USER_DISABLED(1002, "账号已被禁用"),
    TOKEN_INVALID(1003, "无效或过期的令牌"),
    USERNAME_EXISTS(1004, "用户名已存在"),
    REGISTRATION_DUPLICATE(1101, "您已报名该活动，请勿重复报名"),
    ACTIVITY_FULL(1102, "活动名额已满"),
    ACTIVITY_NOT_RECRUITING(1103, "活动当前不在报名阶段"),
    ACTIVITY_ALREADY_ENDED(1104, "活动已结束"),
    ACTIVITY_NOT_FOUND(1105, "活动不存在或已删除"),
    ACTIVITY_STATUS_INVALID(1106, "活动状态不允许该操作"),
    REGISTRATION_STATUS_INVALID(1107, "报名状态不允许该操作"),
    NOT_REGISTERED(1201, "您未报名该活动或报名未通过审核"),
    OUTSIDE_SIGNIN_WINDOW(1202, "当前不在签到/签退时间窗口内"),
    ALREADY_CHECKED_IN(1203, "您已完成签到"),
    ALREADY_CHECKED_OUT(1204, "您已完成签退"),
    ATTENDANCE_INVALID(1205, "签到记录无效，工时已作废"),
    NOT_CHECKED_IN(1206, "您尚未签到，无法签退"),

    // 13xx 档案与证明
    CERTIFICATE_NOT_FOUND(1301, "服务证明不存在"),
    CERT_ALREADY_EXISTS(1302, "该周期证明已存在，请勿重复签发"),

    // 14xx 系统管理
    ANNOUNCEMENT_NOT_FOUND(1401, "公告不存在"),
    ROLE_NOT_FOUND(1402, "角色不存在"),
    TYPE_IN_USE(1403, "该活动类型已被活动引用，无法删除"),
    USER_NOT_FOUND(1404, "用户不存在"),
    INVALID_STATUS(1405, "状态参数不合法"),
    CERT_SERIAL_EXISTS(1301, "证明编号冲突，请重试"),

    // 15xx 文件与证明 PDF
    FILE_TYPE_NOT_ALLOWED(1501, "仅支持 jpg/jpeg/png/webp/gif 图片"),
    FILE_TOO_LARGE(1502, "文件大小超过 5MB 限制"),
    FILE_WRITE_FAILED(1503, "文件保存失败，请稍后重试"),
    CERT_PDF_NOT_GENERATED(1504, "该证明尚未生成 PDF，请重新签发");

    private final int code;
    private final String message;
}
