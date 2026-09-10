package com.tripcraft.common;

import lombok.Data;

@Data
public class Result<T> {
    private Integer code; // 业务状态码：200成功，500失败
    private String message; // 提示信息
    private T data; // 返回的业务数据

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(String message) {
        Result<T> r = new Result<>();
        r.setCode(500);
        r.setMessage(message);
        return r;
    }
}
