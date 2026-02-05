package miu.cs544.releasesystem.notification.service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.mail.default-domain:example.com}")
    private String defaultDomain;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Returns true if SMTP is configured with a real account and password.
     */
    private boolean isSmtpConfigured() {
        return smtpUsername != null && !smtpUsername.isBlank()
                && smtpPassword != null && !smtpPassword.isBlank();
    }

    public void sendEmail(String recipient, String subject, String body) {
        String toAddress = recipient.contains("@") ? recipient : recipient + "@" + defaultDomain;

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toAddress);
            helper.setSubject(subject);
            helper.setText(body, false);

            if (isSmtpConfigured()) {
                mailSender.send(message);
                log.info("Email sent to {}: {}", toAddress, subject);
            } else {
                log.info("Email sending disabled (SMTP not configured) - would have sent to {}: {}", toAddress, subject);
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toAddress, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
