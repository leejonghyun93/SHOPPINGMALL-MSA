package org.kosa.notificationservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * 🔧 이메일 설정 - Gmail SMTP 전용
 */
@Configuration
@Slf4j
public class SimpleEmailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    /**
     * 🔥 실제 Gmail JavaMailSender (조건 수정)
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "notification.email.enabled", havingValue = "true", matchIfMissing = true)
    public JavaMailSender realJavaMailSender() {
        log.info("🔧 실제 Gmail JavaMailSender 생성 - 이메일 발송 활성화");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Gmail SMTP 설정
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        log.info("📧 Gmail 서버: {}:{}", host, port);
        log.info("📧 Gmail 사용자: {}", maskEmail(username));
        log.info("📧 Gmail 비밀번호 길이: {}", password != null ? password.length() : 0);

        // 🔥 Gmail SMTP Properties (테스트에서 성공한 설정과 동일)
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", "true"); // 디버그 모드

        log.info("✅ Gmail JavaMailSender 설정 완료");

        return mailSender;
    }

    /**
     * 🔧 더미 JavaMailSender - 이메일 비활성화시만 사용
     */
    @Bean
    @ConditionalOnProperty(name = "notification.email.enabled", havingValue = "false")
    public JavaMailSender dummyJavaMailSender() {
        log.warn("🔧 더미 JavaMailSender 생성 - 이메일 발송 비활성화");
        log.warn("⚠️  실제 이메일이 발송되지 않습니다!");

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // 더미 설정
        mailSender.setHost("localhost");
        mailSender.setPort(25);
        mailSender.setUsername("dummy");
        mailSender.setPassword("dummy");

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "false");
        props.put("mail.debug", "false");

        return mailSender;
    }

    /**
     * 이메일 마스킹 (로그용)
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty() || !email.contains("@")) {
            return "설정되지 않음";
        }

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return localPart + "@" + domain;
        }

        return localPart.substring(0, 2) + "***@" + domain;
    }
}