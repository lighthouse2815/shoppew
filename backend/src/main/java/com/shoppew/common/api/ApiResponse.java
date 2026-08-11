package com.shoppew.common.api;

import java.time.Clock;
import java.time.Instant;

public record ApiResponse<T>(boolean success, T data, ApiError error, Instant timestamp) {

    public static <T> ApiResponse<T> success(T data, Clock clock) {
        return new ApiResponse<>(true, data, null, Instant.now(clock));
    }

    public static ApiResponse<Void> failure(ApiError error, Clock clock) {
        return new ApiResponse<>(false, null, error, Instant.now(clock));
    }
}
