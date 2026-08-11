package com.shoppew.notification.sender;

import com.shoppew.common.config.AppProperties;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
@ConditionalOnProperty(name = "app.email.delivery-enabled", havingValue = "true")
public class SmtpEmailNotificationSender implements EmailNotificationSender {

    private final JavaMailSender mailSender;
    private final AppProperties properties;

    public SmtpEmailNotificationSender(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public NotificationSendResult send(NotificationMessage notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.email().from());
            helper.setTo(notification.recipientEmail());
            helper.setSubject(notification.title());
            helper.setText(
                    notification.body(),
                    "<p>" + HtmlUtils.htmlEscape(notification.body()).replace("\n", "<br>") + "</p>");
            String reference = "smtp-" + UUID.randomUUID();
            message.setHeader("X-Shoppew-Notification-Id", reference);
            mailSender.send(message);
            return NotificationSendResult.delivered(reference);
        } catch (Exception exception) {
            throw new NotificationSendException("SMTP delivery failed", exception);
        }
    }
}
