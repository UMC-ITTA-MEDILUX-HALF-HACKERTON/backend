package com.allbutyou.common.response;

import com.allbutyou.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 응답 래퍼")
public record ApiResponse<T>(
        @Schema(description = "성공 여부", example = "true") boolean success,
        @Schema(description = "응답 데이터 (실패 시 null)") T data,
        @Schema(description = "에러 코드 (성공 시 null)", example = "null", nullable = true) String errorCode,
        @Schema(description = "에러 메시지 (성공 시 null)", example = "null", nullable = true) String message
) {

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
