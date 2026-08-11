package com.shoppew.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HttpUrlValidatorTest {

    private final HttpUrlValidator validator = new HttpUrlValidator();

    @Test
    void acceptsOptionalHttpAndHttpsUrls() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("http://localhost:9000/shoppew-media/image.png", null)).isTrue();
        assertThat(validator.isValid("https://media.shoppew.example/image.webp", null)).isTrue();
    }

    @Test
    void rejectsScriptDataFileAndCredentialBearingUrls() {
        assertThat(validator.isValid("javascript:alert(1)", null)).isFalse();
        assertThat(validator.isValid("data:image/svg+xml;base64,PHN2Zz4=", null)).isFalse();
        assertThat(validator.isValid("file:///etc/passwd", null)).isFalse();
        assertThat(validator.isValid("https://user:password@media.shoppew.example/image.png", null)).isFalse();
        assertThat(validator.isValid("https:\\attacker.example\\image.png", null)).isFalse();
    }
}
