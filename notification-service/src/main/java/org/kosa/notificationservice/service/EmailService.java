package org.kosa.notificationservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.dto.NotificationMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@livecast.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * 방송 시작 알림 이메일 발송
     */
    public void sendBroadcastStartNotification(NotificationMessageDto message, String userEmail) {
        try {
            String subject = createSubject(message);
            String htmlContent = createHtmlContent(message);

            sendHtmlEmail(userEmail, subject, htmlContent);

            log.info("방송 시작 알림 이메일 발송 완료: userId={}, email={}",
                    message.getUserId(), userEmail);

        } catch (Exception e) {
            log.error("방송 시작 알림 이메일 발송 실패: userId={}, error={}",
                    message.getUserId(), e.getMessage(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    /**
     * 일반 알림 이메일 발송
     */
    public void sendGeneralNotification(NotificationMessageDto message, String userEmail) {
        try {
            sendSimpleEmail(userEmail, message.getTitle(), message.getMessage());

            log.info("일반 알림 이메일 발송 완료: userId={}, type={}",
                    message.getUserId(), message.getType());

        } catch (Exception e) {
            log.error("일반 알림 이메일 발송 실패: userId={}, error={}",
                    message.getUserId(), e.getMessage(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    /**
     * 단순 텍스트 이메일 발송
     */
    private void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    /**
     * HTML 이메일 발송
     */
    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // HTML 형식

        mailSender.send(mimeMessage);
    }

    /**
     * 이메일 제목 생성
     */
    private String createSubject(NotificationMessageDto message) {
        switch (message.getType()) {
            case "BROADCAST_START":
                return "🔴 " + message.getTitle() + " - 방송이 시작되었습니다!";
            case "BROADCAST_END":
                return "⏹️ " + message.getTitle() + " - 방송이 종료되었습니다";
            case "BROADCAST_REMINDER":
                return "⏰ " + message.getTitle() + " - 곧 방송이 시작됩니다";
            default:
                return message.getTitle();
        }
    }

    /**
     * HTML 이메일 내용 생성
     */
    private String createHtmlContent(NotificationMessageDto message) {
        String broadcastUrl = baseUrl + "/broadcast/" + message.getBroadcastId();

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    .container { max-width: 600px; margin: 0 auto; font-family: Arial, sans-serif; }
                    .header { background-color: #FF4B4B; color: white; padding: 20px; text-align: center; }
                    .content { padding: 30px; background-color: #f9f9f9; }
                    .button { 
                        display: inline-block; 
                        background-color: #FF4B4B; 
                        color: white; 
                        padding: 12px 30px; 
                        text-decoration: none; 
                        border-radius: 5px; 
                        margin: 20px 0;
                    }
                    .footer { padding: 20px; text-align: center; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔴 라이브 방송 알림</h1>
                    </div>
                    
                    <div class="content">
                        <h2>%s</h2>
                        <p>%s</p>
                        
                        <div style="text-align: center;">
                            <a href="%s" class="button">방송 보러가기</a>
                        </div>
                        
                        <p style="color: #666; font-size: 14px;">
                            방송 시간: %s<br>
                            알림 우선순위: %s
                        </p>
                    </div>
                    
                    <div class="footer">
                        <p>LiveCast 알림 서비스</p>
                        <p>더 이상 알림을 받고 싶지 않으시면 
                           <a href="%s/unsubscribe?token=%s">구독 해지</a>를 클릭하세요.
                        </p>
                    </div>
                </div>
            </body>
            </html>
            """,
                message.getTitle(),
                message.getMessage(),
                broadcastUrl,
                message.getCreatedAt().toString(),
                message.getPriority(),
                baseUrl,
                generateUnsubscribeToken(message.getUserId())
        );
    }

    /**
     * 구독 해지 토큰 생성 (간단한 예시)
     */
    private String generateUnsubscribeToken(Long userId) {
        // 실제로는 JWT나 더 안전한 토큰 생성 방식 사용
        return "token_" + userId + "_" + System.currentTimeMillis();
    }

    /**
     * 이메일 유효성 검사
     */
    public boolean isValidEmail(String email) {
        return StringUtils.hasText(email) && email.contains("@") && email.contains(".");
    }
}