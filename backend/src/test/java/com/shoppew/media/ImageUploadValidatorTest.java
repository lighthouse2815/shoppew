package com.shoppew.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.shoppew.common.exception.ApiException;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ImageUploadValidatorTest {

    private static final byte[] VALID_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
    private final ImageUploadValidator validator = new ImageUploadValidator();

    @Test
    void acceptsPngWhenDeclaredTypeAndSignatureMatch() {
        ImageUploadValidator.ValidatedImage result = validator.validate(
                new MockMultipartFile("file", "product.png", "image/png", VALID_PNG));

        assertThat(result.bytes()).isEqualTo(VALID_PNG);
        assertThat(result.contentType()).isEqualTo("image/png");
        assertThat(result.extension()).isEqualTo("png");
    }

    @Test
    void rejectsTruncatedImageThatOnlyContainsPngSignature() {
        byte[] truncated = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

        assertInvalidImage(new MockMultipartFile("file", "truncated.png", "image/png", truncated));
    }

    @Test
    void rejectsContentTypeSpoofing() {
        byte[] executable = {'M', 'Z', 0x00, 0x02};

        assertInvalidImage(new MockMultipartFile("file", "product.png", "image/png", executable));
    }

    @Test
    void rejectsUnsupportedFormats() {
        byte[] gif = {'G', 'I', 'F', '8', '9', 'a'};

        assertInvalidImage(new MockMultipartFile("file", "product.gif", "image/gif", gif));
    }

    @Test
    void rejectsImagesLargerThanFiveMegabytes() {
        byte[] oversized = new byte[(int) ImageUploadValidator.MAX_IMAGE_BYTES + 1];

        assertInvalidImage(new MockMultipartFile("file", "large.png", "image/png", oversized));
    }

    private void assertInvalidImage(MockMultipartFile file) {
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).code())
                .isEqualTo("INVALID_IMAGE_UPLOAD");
    }
}
