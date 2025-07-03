package org.kosa.notificationservice.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 환경변수 로딩 확인용 컴포넌트
 */
@Component
@Slf4j
public class EnvironmentChecker {

    @Value("${MAIL_USERNAME:NOT_SET}")
    private String mailUsername;

    @Value("${MAIL_PASSWORD:NOT_SET}")
    private String mailPassword;

    @Value("${spring.mail.username:NOT_SET}")
    private String springMailUsername;

    @Value("${spring.mail.password:NOT_SET}")
    private String springMailPassword;

    @PostConstruct
    public void checkEnvironmentVariables() {
        log.info("=== 환경변수 및 설정 확인 ===");
        log.info("MAIL_USERNAME 환경변수: {}", "NOT_SET".equals(mailUsername) ? "❌ 설정안됨" : "✅ 설정됨");
        log.info("MAIL_PASSWORD 환경변수: {}", "NOT_SET".equals(mailPassword) ? "❌ 설정안됨" : "✅ 설정됨");
        log.info("spring.mail.username: {}", "NOT_SET".equals(springMailUsername) ? "❌ 설정안됨" : maskEmail(springMailUsername));
        log.info("spring.mail.password: {}", "NOT_SET".equals(springMailPassword) ? "❌ 설정안됨" : "✅ 설정됨 (길이: " + springMailPassword.length() + ")");

        if ("NOT_SET".equals(mailUsername) || "NOT_SET".equals(mailPassword)) {
            log.error("🚨 환경변수가 제대로 설정되지 않았습니다!");
            log.error("IntelliJ Run Configuration에서 Environment variables 설정을 확인하세요:");
            log.error("MAIL_USERNAME=fhohffodf93@gmail.com;MAIL_PASSWORD=16자리앱비밀번호");
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