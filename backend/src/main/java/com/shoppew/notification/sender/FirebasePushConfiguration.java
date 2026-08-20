package com.shoppew.notification.sender;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.shoppew.common.config.AppProperties;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.push", name = "delivery-enabled", havingValue = "true")
public class FirebasePushConfiguration {
    @Bean(destroyMethod = "delete")
    FirebaseApp shoppewFirebaseApp(AppProperties properties) throws IOException {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setProjectId(properties.push().projectId())
                .build();
        return FirebaseApp.initializeApp(options, "shoppew-push");
    }

    @Bean
    FirebaseMessaging shoppewFirebaseMessaging(FirebaseApp shoppewFirebaseApp) {
        return FirebaseMessaging.getInstance(shoppewFirebaseApp);
    }
}
