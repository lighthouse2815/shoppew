package com.shoppew.notification.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoppew.common.config.AppProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEmailNotificationSenderTests {

    @Test
    void sendsMultipartAlternativeEmailThroughConfiguredSender() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        AppProperties properties = mock(AppProperties.class);
        when(properties.email()).thenReturn(new AppProperties.Email(
                true,
                "no-reply@shoppew.local",
                "http://localhost:3000"));

        SmtpEmailNotificationSender sender = new SmtpEmailNotificationSender(mailSender, properties);
        NotificationSendResult result = sender.send(new NotificationMessage(
                UUID.randomUUID(),
                "buyer@example.test",
                "Xác nhận đơn hàng",
                "Đơn hàng của bạn đã được ghi nhận.",
                Map.of("orderId", UUID.randomUUID().toString())));

        assertThat(result.status()).isEqualTo(NotificationSendResult.Status.DELIVERED);
        assertThat(result.providerReference()).startsWith("smtp-");
        mimeMessage.saveChanges();
        assertThat(mimeMessage.getContentType()).startsWith("multipart/mixed");
        assertThat(mimeMessage.getHeader("X-Shoppew-Notification-Id", null)).isEqualTo(result.providerReference());
        verify(mailSender).send(any(MimeMessage.class));
    }
}
