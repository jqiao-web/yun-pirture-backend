package com.qiao.yunpicturebackend.common;

import com.qiao.yunpicturebackend.exception.ErrorCode;
import lombok.Getter;

import java.io.Serializable;

/**
 * 全局响应封装
 * @param <T>
 */
@Getter
public class BaseResponse<T> implements Serializable {
    private int code;
    private T data;
    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
