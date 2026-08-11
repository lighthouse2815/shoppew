package com.shoppew.auth.email;

import com.shoppew.common.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.delivery-enabled", havingValue = "true")
class SmtpAuthenticationEmailGateway implements AuthenticationEmailGateway {

    private static final Logger log = LoggerFactory.getLogger(SmtpAuthenticationEmailGateway.class);
    private final JavaMailSender mailSender;
    private final AppProperties properties;

    SmtpAuthenticationEmailGateway(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void sendEmailVerification(String recipient, String rawToken) {
        String link = properties.email().webBaseUrl() + "/verify-email?token=" + encode(rawToken);
        send(
                recipient,
                "Xác minh email shoppew",
                "Mở liên kết sau để xác minh email shoppew của bạn: " + link,
                "<p>Chào bạn,</p><p>Hãy <a href=\"" + link + "\">xác minh email shoppew</a>. Liên kết chỉ dùng được một lần.</p>");
    }

    @Override
    public void sendPasswordReset(String recipient, String rawToken) {
        String link = properties.email().webBaseUrl() + "/reset-password?token=" + encode(rawToken);
        send(
                recipient,
                "Đặt lại mật khẩu shoppew",
                "Mở liên kết sau để đặt lại mật khẩu shoppew của bạn: " + link,
                "<p>Chào bạn,</p><p>Hãy <a href=\"" + link + "\">đặt lại mật khẩu shoppew</a>. Liên kết chỉ dùng được một lần.</p>");
    }

    private void send(String recipient, String subject, String plainText, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.email().from());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(plainText, html);
            mailSender.send(message);
        } catch (MailException | jakarta.mail.MessagingException exception) {
            log.error("Authentication email delivery failed ({})", exception.getClass().getSimpleName());
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
