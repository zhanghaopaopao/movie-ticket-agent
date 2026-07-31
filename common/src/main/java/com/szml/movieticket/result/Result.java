package com.szml.movieticket.result;

import lombok.Data;

/**
 * 通用 API 响应包装。
 *
 * @param <T> 响应数据的类型
 * @author zhanghao
 * @since 2026-07-30
 */
@Data
public class Result<T> {

    /** 业务状态码，0 表示成功 */
    private int code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "ok";
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "ok";
        result.data = data;
        return result;
    }

    public static <T> Result<T> error(int code, String msg) {
        Result<T> result = new Result<>();
        result.code = code;
        result.msg = msg;
        return result;
    }
}
