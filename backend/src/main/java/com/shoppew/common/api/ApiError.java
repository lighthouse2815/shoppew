package com.shoppew.common.api;

import java.util.List;

public record ApiError(String code, String message, List<ErrorDetail> details) {

    public ApiError {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
