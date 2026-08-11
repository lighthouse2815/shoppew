package com.shoppew.media;

import com.shoppew.common.config.AppProperties;
import com.shoppew.common.exception.ApiException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
class S3StorageService implements StorageService {

    private static final long MULTIPART_PART_SIZE = 5L * 1024 * 1024;
    private final MinioClient client;
    private final AppProperties.Storage properties;

    S3StorageService(MinioClient client, AppProperties appProperties) {
        this.client = client;
        this.properties = appProperties.storage();
    }

    @Override
    public StoredObject upload(
            String objectKey,
            InputStream inputStream,
            long contentLength,
            String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(inputStream, contentLength, MULTIPART_PART_SIZE)
                    .build());
            return new StoredObject(objectKey, publicUrl(objectKey));
        } catch (MinioException exception) {
            throw storageUnavailable();
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (MinioException exception) {
            throw storageUnavailable();
        }
    }

    @Override
    public String presignedDownloadUrl(String objectKey, Duration validity) {
        long seconds = validity.toSeconds();
        if (seconds < 1 || seconds > TimeUnit.DAYS.toSeconds(7)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL_VALIDITY", "Thời hạn URL không hợp lệ");
        }
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .method(Http.Method.GET)
                    .expiry(Math.toIntExact(seconds))
                    .build());
        } catch (MinioException exception) {
            throw storageUnavailable();
        }
    }

    private String publicUrl(String objectKey) {
        String endpoint = properties.publicEndpoint().replaceAll("/+$", "");
        return endpoint + "/" + properties.bucket() + "/" + objectKey;
    }

    private ApiException storageUnavailable() {
        return new ApiException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "OBJECT_STORAGE_UNAVAILABLE",
                "Kho ảnh tạm thời không khả dụng");
    }
}
