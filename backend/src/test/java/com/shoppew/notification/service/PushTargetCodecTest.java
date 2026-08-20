package com.shoppew.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.shoppew.common.config.AppProperties;
import org.junit.jupiter.api.Test;

class PushTargetCodecTest {
    @Test
    void encryptsWithAuthenticatedContextAndRejectsTampering() {
        AppProperties properties = mock(AppProperties.class);
        when(properties.push()).thenReturn(new AppProperties.Push(
                false,
                "",
                "c2hvcHBldy1kZXYtcHVzaC1lbmNyeXB0aW9uLWtleSE="));
        PushTargetCodec codec = new PushTargetCodec(properties);
        String target = "firebase-installation-id-codec-test-2026";
        String hash = codec.hash(target);

        String encrypted = codec.encrypt(target, hash);

        assertThat(encrypted).doesNotContain(target);
        assertThat(codec.decrypt(encrypted, hash)).isEqualTo(target);
        assertThatThrownBy(() -> codec.decrypt(encrypted, codec.hash(target + "-other")))
                .isInstanceOf(IllegalStateException.class);
    }
}
