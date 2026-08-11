package com.shoppew.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            Clock.fixed(Instant.parse("2026-08-11T00:00:00Z"), ZoneOffset.UTC));

    @Test
    void oversizedMultipartUsesStablePayloadTooLargeResponse() {
        var response = handler.handleOversizedUpload(new MaxUploadSizeExceededException(1024));

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("UPLOAD_TOO_LARGE");
    }

    @Test
    void unexpectedFailureDoesNotExposeExceptionMessageToClient() {
        var response = handler.handleUnexpected(new IllegalStateException("credential-value-must-stay-private"));

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().message()).doesNotContain("credential-value");
    }
}
