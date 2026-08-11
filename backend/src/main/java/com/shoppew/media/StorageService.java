package com.shoppew.media;

import java.io.InputStream;
import java.time.Duration;

public interface StorageService {

    StoredObject upload(
            String objectKey,
            InputStream inputStream,
            long contentLength,
            String contentType);

    void delete(String objectKey);

    String presignedDownloadUrl(String objectKey, Duration validity);

    record StoredObject(String objectKey, String publicUrl) {}
}
