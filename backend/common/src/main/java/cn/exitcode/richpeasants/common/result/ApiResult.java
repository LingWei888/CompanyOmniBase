package cn.exitcode.richpeasants.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResult<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResult<T> ok(T data) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static ApiResult<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResult<T> fail(ResultCode resultCode) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(resultCode.getCode());
        result.setMessage(resultCode.getMessage());
        return result;
    }

    public static <T> ApiResult<T> fail(int code, String message) {
        ApiResult<T> result = new ApiResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
