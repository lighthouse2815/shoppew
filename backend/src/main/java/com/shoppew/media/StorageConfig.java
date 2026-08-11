package com.shoppew.media;

import com.shoppew.common.config.AppProperties;
import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class StorageConfig {

    @Bean
    MinioClient minioClient(AppProperties properties) {
        AppProperties.Storage storage = properties.storage();
        return MinioClient.builder()
                .endpoint(storage.endpoint())
                .credentials(storage.accessKey(), storage.secretKey())
                .build();
    }
}
