package org.kosa.livestreamingservice.controller.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Properties;

/**
 * Gmail SMTP 연결 및 발송 테스트 컨트롤러
 */
@RestController
@RequestMapping("/api/test/gmail")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GmailConnectionTest {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String mailUsername;

    @Value("${spring.mail.password}")
    private String mailPassword;

    /**
     * 🔥 현재 설정된 Gmail 정보 확인
     */
    @GetMapping("/config-check")
    public Map<String, Object> checkGmailConfig() {
        return Map.of(
                "username", maskEmail(mailUsername),
                "password_length", mailPassword != null ? mailPassword.length() : 0,
                "password_set", mailPassword != null && !mailPassword.isEmpty(),
                "expected_password_length", 16
        );
    }

    /**
     * 🔥 Gmail SMTP 연결 테스트 (순수 JavaMail)
     */
    @PostMapping("/connection-test")
    public Map<String, Object> testGmailConnection() {
        try {
            log.info("Gmail SMTP 연결 테스트 시작: {}", maskEmail(mailUsername));

            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailUsername, mailPassword);
                }
            });

            // 실제 SMTP 연결 테스트
            Transport transport = session.getTransport("smtp");
            transport.connect("smtp.gmail.com", 587, mailUsername, mailPassword);
            transport.close();

            log.info("✅ Gmail SMTP 연결 성공!");
            return Map.of(
                    "status", "success",
                    "message", "Gmail SMTP 연결 성공!",
                    "username", maskEmail(mailUsername),
                    "server", "smtp.gmail.com:587"
            );

        } catch (Exception e) {
            log.error("❌ Gmail SMTP 연결 실패: {}", e.getMessage());

            String errorMessage = e.getMessage();
            String solution = "";

            if (errorMessage.contains("Authentication failed")) {
                solution = "앱 비밀번호를 새로 생성하고 16자리 연속으로 입력하세요";
            } else if (errorMessage.contains("Connection refused")) {
                solution = "네트워크/방화벽 설정을 확인하세요";
            }

            return Map.of(
                    "status", "error",
                    "message", "Gmail SMTP 연결 실패: " + errorMessage,
                    "solution", solution,
                    "username", maskEmail(mailUsername),
                    "password_length", mailPassword != null ? mailPassword.length() : 0
            );
        }
    }

    /**
     * 🔥 실제 테스트 이메일 발송 (Spring JavaMailSender 사용)
     */
    @PostMapping("/send-test")
    public Map<String, Object> sendTestEmail(@RequestParam String toEmail) {
        try {
            log.info("테스트 이메일 발송 시작: {} -> {}", maskEmail(mailUsername), toEmail);

            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom(new InternetAddress(mailUsername));
            message.setRecipients(MimeMessage.RecipientType.TO, toEmail);
            message.setSubject("🧪 Gmail SMTP 연결 테스트");
            message.setText("Gmail SMTP 연결이 성공했습니다!\n\n현재 시간: " + java.time.LocalDateTime.now());

            javaMailSender.send(message);

            log.info("✅ 테스트 이메일 발송 성공!");
            return Map.of(
                    "status", "success",
                    "message", "테스트 이메일 발송 성공!",
                    "from", maskEmail(mailUsername),
                    "to", toEmail
            );

        } catch (Exception e) {
            log.error("❌ 테스트 이메일 발송 실패: {}", e.getMessage());

            return Map.of(
                    "status", "error",
                    "message", "이메일 발송 실패: " + e.getMessage(),
                    "from", maskEmail(mailUsername)
            );
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        if (localPart.length() <= 2) return email;
        return localPart.substring(0, 2) + "***@" + domain;
    }
}