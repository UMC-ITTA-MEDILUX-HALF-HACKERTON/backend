package com.allbutyou.common.response;

import com.allbutyou.common.exception.ErrorCode;

public record ApiResponse<T>(boolean success, T data, String errorCode, String message) {

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode.name(), message);
    }
}
