package com.shoppew.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    private static final Instant NOW = Instant.parse("2026-08-10T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void successUsesTheProvidedClockAndContainsNoError() {
        ApiResponse<Map<String, String>> response = ApiResponse.success(Map.of("currency", "VND"), CLOCK);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).containsEntry("currency", "VND");
        assertThat(response.error()).isNull();
        assertThat(response.timestamp()).isEqualTo(NOW);
    }

    @Test
    void failureUsesStableErrorShape() {
        ApiError error = new ApiError("PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm", null);

        ApiResponse<Void> response = ApiResponse.failure(error, CLOCK);

        assertThat(response.success()).isFalse();
        assertThat(response.data()).isNull();
        assertThat(response.error().details()).isEmpty();
        assertThat(response.timestamp()).isEqualTo(NOW);
    }
}
