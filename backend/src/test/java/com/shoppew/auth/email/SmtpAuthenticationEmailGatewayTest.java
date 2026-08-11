package com.shoppew.auth.email;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.shoppew.common.config.AppProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpAuthenticationEmailGatewayTest {

    @Test
    void createsMultipartAlternativeMessageForPasswordReset() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        AppProperties properties = mock(AppProperties.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        when(properties.email()).thenReturn(new AppProperties.Email(
                true,
                "no-reply@shoppew.local",
                "http://localhost:3000"));

        SmtpAuthenticationEmailGateway gateway = new SmtpAuthenticationEmailGateway(mailSender, properties);
        gateway.sendPasswordReset("customer@example.vn", "one-time-token");

        verify(mailSender).send(message);
        org.assertj.core.api.Assertions.assertThat(message.getContent()).isInstanceOf(MimeMultipart.class);
    }
}
