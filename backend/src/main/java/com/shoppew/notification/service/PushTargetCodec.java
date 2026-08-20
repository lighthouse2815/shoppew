package com.shoppew.notification.service;

import com.shoppew.common.config.AppProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PushTargetCodec {
    private static final byte VERSION = 1;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public PushTargetCodec(AppProperties properties) {
        try {
            byte[] decoded = Base64.getDecoder().decode(properties.push().encryptionKey());
            if (decoded.length != 32) throw new IllegalArgumentException("must decode to exactly 32 bytes");
            this.key = new SecretKeySpec(decoded, "AES");
        } catch (RuntimeException exception) {
            throw new IllegalStateException("APP_PUSH_ENCRYPTION_KEY must be a Base64-encoded 32-byte key", exception);
        }
    }

    public String hash(String target) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(target.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public String encrypt(String target, String targetHash) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(targetHash.getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(target.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ByteBuffer.allocate(1 + iv.length + ciphertext.length)
                            .put(VERSION).put(iv).put(ciphertext).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt push target", exception);
        }
    }

    public String decrypt(String encryptedTarget, String targetHash) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode(encryptedTarget));
            if (buffer.remaining() <= 1 + IV_BYTES || buffer.get() != VERSION) {
                throw new GeneralSecurityException("Unsupported encrypted push-target value");
            }
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(targetHash.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt push target", exception);
        }
    }
}
