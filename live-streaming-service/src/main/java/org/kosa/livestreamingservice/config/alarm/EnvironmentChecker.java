package org.kosa.livestreamingservice.config.alarm;

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
        if ("NOT_SET".equals(mailUsername) || "NOT_SET".equals(mailPassword)) {

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