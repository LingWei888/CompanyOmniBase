package cn.exitcode.richpeasants.common.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;
}
