package com.shoppew.media;

import com.shoppew.common.exception.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageUploadValidator {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    public static final long MAX_IMAGE_PIXELS = 25_000_000L;
    public static final int MAX_IMAGE_DIMENSION = 10_000;
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("Ảnh tải lên không được để trống");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw invalid("Ảnh tải lên không được vượt quá 5 MB");
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw invalid("Chỉ hỗ trợ ảnh JPEG, PNG hoặc WebP");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw invalid("Không thể đọc ảnh tải lên");
        }
        if (!matchesSignature(bytes, contentType)) {
            throw invalid("Nội dung tệp không khớp với định dạng ảnh khai báo");
        }
        if (!hasValidStructure(bytes, contentType)) {
            throw invalid("Tệp ảnh bị lỗi hoặc có kích thước không an toàn");
        }
        return new ValidatedImage(bytes, contentType, extension);
    }

    private boolean matchesSignature(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && unsigned(bytes[0]) == 0xFF
                    && unsigned(bytes[1]) == 0xD8
                    && unsigned(bytes[2]) == 0xFF;
            case "image/png" -> bytes.length >= 8
                    && unsigned(bytes[0]) == 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47
                    && bytes[4] == 0x0D
                    && bytes[5] == 0x0A
                    && bytes[6] == 0x1A
                    && bytes[7] == 0x0A;
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R'
                    && bytes[1] == 'I'
                    && bytes[2] == 'F'
                    && bytes[3] == 'F'
                    && bytes[8] == 'W'
                    && bytes[9] == 'E'
                    && bytes[10] == 'B'
                    && bytes[11] == 'P';
            default -> false;
        };
    }

    private boolean hasValidStructure(byte[] bytes, String contentType) {
        return switch (contentType) {
            case "image/jpeg", "image/png" -> validateWithImageIo(bytes, contentType);
            case "image/webp" -> validateWebp(bytes);
            default -> false;
        };
    }

    private boolean validateWithImageIo(byte[] bytes, String contentType) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) return false;
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) return false;
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String expectedFormat = contentType.equals("image/png") ? "png" : "jpeg";
                if (!reader.getFormatName().equalsIgnoreCase(expectedFormat)) return false;
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (!safeDimensions(width, height)) return false;
                BufferedImage decoded = reader.read(0);
                return decoded != null
                        && decoded.getWidth() == width
                        && decoded.getHeight() == height;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private boolean validateWebp(byte[] bytes) {
        if (bytes.length < 20 || littleEndianUnsignedInt(bytes, 4) != bytes.length - 8L) return false;
        boolean dimensionsFound = false;
        int offset = 12;
        while (offset + 8 <= bytes.length) {
            String chunkType = new String(bytes, offset, 4, StandardCharsets.US_ASCII);
            long chunkSize = littleEndianUnsignedInt(bytes, offset + 4);
            long dataOffset = offset + 8L;
            long chunkEnd = dataOffset + chunkSize;
            if (chunkSize > Integer.MAX_VALUE || chunkEnd > bytes.length) return false;
            int data = Math.toIntExact(dataOffset);
            int size = Math.toIntExact(chunkSize);
            if (!dimensionsFound) {
                int[] dimensions = webpDimensions(bytes, chunkType, data, size);
                if (dimensions != null) {
                    if (!safeDimensions(dimensions[0], dimensions[1])) return false;
                    dimensionsFound = true;
                }
            }
            long next = chunkEnd + (chunkSize & 1L);
            if (next > bytes.length) return false;
            offset = Math.toIntExact(next);
        }
        return dimensionsFound && offset == bytes.length;
    }

    private int[] webpDimensions(byte[] bytes, String chunkType, int data, int size) {
        return switch (chunkType) {
            case "VP8X" -> size < 10 ? null : new int[] {
                    1 + littleEndian24(bytes, data + 4),
                    1 + littleEndian24(bytes, data + 7)};
            case "VP8L" -> {
                if (size < 5 || unsigned(bytes[data]) != 0x2F) yield null;
                int b1 = unsigned(bytes[data + 1]);
                int b2 = unsigned(bytes[data + 2]);
                int b3 = unsigned(bytes[data + 3]);
                int b4 = unsigned(bytes[data + 4]);
                yield new int[] {
                        1 + b1 + ((b2 & 0x3F) << 8),
                        1 + ((b2 & 0xC0) >> 6) + (b3 << 2) + ((b4 & 0x0F) << 10)};
            }
            case "VP8 " -> {
                if (size < 10
                        || unsigned(bytes[data + 3]) != 0x9D
                        || unsigned(bytes[data + 4]) != 0x01
                        || unsigned(bytes[data + 5]) != 0x2A) yield null;
                int width = (unsigned(bytes[data + 6]) | (unsigned(bytes[data + 7]) << 8)) & 0x3FFF;
                int height = (unsigned(bytes[data + 8]) | (unsigned(bytes[data + 9]) << 8)) & 0x3FFF;
                yield new int[] {width, height};
            }
            default -> null;
        };
    }

    private boolean safeDimensions(int width, int height) {
        return width > 0
                && height > 0
                && width <= MAX_IMAGE_DIMENSION
                && height <= MAX_IMAGE_DIMENSION
                && (long) width * height <= MAX_IMAGE_PIXELS;
    }

    private long littleEndianUnsignedInt(byte[] bytes, int offset) {
        return unsigned(bytes[offset])
                | ((long) unsigned(bytes[offset + 1]) << 8)
                | ((long) unsigned(bytes[offset + 2]) << 16)
                | ((long) unsigned(bytes[offset + 3]) << 24);
    }

    private int littleEndian24(byte[] bytes, int offset) {
        return unsigned(bytes[offset])
                | (unsigned(bytes[offset + 1]) << 8)
                | (unsigned(bytes[offset + 2]) << 16);
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_UPLOAD", message);
    }

    public record ValidatedImage(byte[] bytes, String contentType, String extension) {}
}
