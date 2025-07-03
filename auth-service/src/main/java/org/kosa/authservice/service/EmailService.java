package org.kosa.authservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        value = "spring.mail.username",
        havingValue = "dummy@gmail.com",
        matchIfMissing = false
)
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.name:서비스}")
    private String appName;

    /**
     * 이메일 발송
     */
    public boolean sendEmail(String to, String subject, String content) {
        // 더미 이메일 설정인 경우 실제 발송하지 않음
        if ("dummy@gmail.com".equals(fromEmail)) {
            log.info("📧 [개발모드] 이메일 발송 시뮬레이션: to={}, subject={}", to, subject);
            log.info("📧 [개발모드] 내용: {}", content);
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, appName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);

            mailSender.send(message);
            log.info("이메일 발송 성공: to={}, subject={}", to, subject);

            return true;

        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            return false;
        }
    }

    /**
     * HTML 이메일 발송
     */
    public boolean sendHtmlEmail(String to, String subject, String htmlContent) {
        // 더미 이메일 설정인 경우 실제 발송하지 않음
        if ("dummy@gmail.com".equals(fromEmail)) {
            log.info("📧 [개발모드] HTML 이메일 발송 시뮬레이션: to={}, subject={}", to, subject);
            log.info("📧 [개발모드] HTML 내용: {}", htmlContent);
            return true;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail, appName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML 이메일 발송 성공: to={}, subject={}", to, subject);

            return true;

        } catch (Exception e) {
            log.error("HTML 이메일 발송 실패: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            return false;
        }
    }
}