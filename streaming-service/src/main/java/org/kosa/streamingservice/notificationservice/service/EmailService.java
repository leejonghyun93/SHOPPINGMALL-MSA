package org.kosa.streamingservice.notificationservice.service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.config.NotificationProperties;
import org.kosa.notificationservice.config.ExternalServiceProperties;
import org.kosa.notificationservice.dto.NotificationMessageDto;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final NotificationProperties notificationProperties;
    private final ExternalServiceProperties externalServiceProperties;

    /**
     * 🔔 방송 시작 알림 이메일 발송 (기존 메서드)
     */
    public void sendBroadcastStartNotification(String toEmail, String userName,
                                               String broadcastTitle, String broadcasterName,
                                               Long broadcastId) {

        if (!notificationProperties.getEmail().getEnabled()) {
            log.info("이메일 알림이 비활성화되어 있습니다.");
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // 🔥 UnsupportedEncodingException 해결 - 간단한 방법
            helper.setFrom(notificationProperties.getEmail().getFromEmail());  // 이메일만 설정

            helper.setTo(toEmail);
            helper.setSubject(String.format("🔴 %s - 방송이 시작되었습니다!", broadcastTitle));

            String emailContent = createEmailHtml(userName, broadcasterName, broadcastTitle, broadcastId);
            helper.setText(emailContent, true);

            javaMailSender.send(message);

            log.info("이메일 발송 완료: toEmail={}, broadcastId={}", toEmail, broadcastId);

        } catch (Exception e) {
            log.error("이메일 발송 실패: toEmail={}, error={}", toEmail, e.getMessage());
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage());
        }
    }

    /**
     * 🔔 Kafka용 방송 시작 알림 이메일 발송 (NotificationMessageDto 버전)
     */
    public void sendBroadcastStartNotification(NotificationMessageDto messageDto, String userEmail) {
        log.info("Kafka 방송 시작 알림 이메일 발송: userId={}, broadcastId={}",
                messageDto.getUserId(), messageDto.getBroadcastId());

        if (!notificationProperties.getEmail().getEnabled()) {
            log.info("이메일 알림이 비활성화되어 있습니다.");
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(notificationProperties.getEmail().getFromEmail());
            helper.setTo(userEmail);
            helper.setSubject(String.format("🔴 %s - 방송이 시작되었습니다!", messageDto.getTitle()));

            String emailContent = createKafkaEmailHtml(messageDto);
            helper.setText(emailContent, true);

            javaMailSender.send(message);

            log.info("Kafka 이메일 발송 완료: userId={}, email={}",
                    messageDto.getUserId(), maskEmail(userEmail));

        } catch (Exception e) {
            log.error("Kafka 이메일 발송 실패: userId={}, error={}",
                    messageDto.getUserId(), e.getMessage());
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage());
        }
    }
    @PostConstruct
    public void init() {
        log.info("📧 From Email = {}", notificationProperties.getEmail().getFromEmail());
        log.info("📧 Email Enabled = {}", notificationProperties.getEmail().getEnabled());
    }
    /**
     * 📧 일반 알림 이메일 발송 (Kafka용)
     */
    public void sendGeneralNotification(NotificationMessageDto messageDto, String userEmail) {
        log.info("일반 알림 이메일 발송: userId={}, type={}",
                messageDto.getUserId(), messageDto.getType());

        if (!notificationProperties.getEmail().getEnabled()) {
            log.info("이메일 알림이 비활성화되어 있습니다.");
            return;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(notificationProperties.getEmail().getFromEmail());
            helper.setTo(userEmail);
            helper.setSubject(messageDto.getTitle() != null ? messageDto.getTitle() : "알림");

            String emailContent = createGeneralEmailHtml(messageDto);
            helper.setText(emailContent, true);

            javaMailSender.send(message);

            log.info("일반 이메일 발송 완료: userId={}, type={}",
                    messageDto.getUserId(), messageDto.getType());

        } catch (Exception e) {
            log.error("일반 이메일 발송 실패: userId={}, error={}",
                    messageDto.getUserId(), e.getMessage());
            throw new RuntimeException("이메일 발송 실패: " + e.getMessage());
        }
    }

    /**
     * HTML 이메일 템플릿 생성 (기존)
     */
    private String createEmailHtml(String userName, String broadcasterName,
                                   String broadcastTitle, Long broadcastId) {

        String broadcastUrl = externalServiceProperties.getFrontendBaseUrl() + "/broadcast/" + broadcastId;
        String unsubscribeUrl = externalServiceProperties.getUnsubscribeUrl();

        return String.format("""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; }
        .container { max-width: 600px; margin: 0 auto; background: white; }
        .header { background: #ff6b6b; color: white; padding: 20px; text-align: center; }
        .content { padding: 20px; }
        .button { 
            background: #ff6b6b; color: white; padding: 12px 24px; 
            text-decoration: none; border-radius: 5px; display: inline-block; 
        }
        .footer { padding: 20px; background: #f8f9fa; font-size: 12px; color: #666; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔴 라이브 방송 시작!</h1>
        </div>
        <div class="content">
            <h2>안녕하세요, %s님!</h2>
            <p><strong>%s</strong>님의 방송이 지금 시작되었습니다!</p>
            
            <h3>📺 방송 정보</h3>
            <ul>
                <li><strong>방송 제목:</strong> %s</li>
                <li><strong>방송자:</strong> %s</li>
                <li><strong>시작 시간:</strong> 지금!</li>
            </ul>
            
            <p>지금 바로 시청하세요!</p>
            <a href="%s" class="button">🎬 방송 보러 가기</a>
        </div>
        <div class="footer">
            <p>%s | <a href="%s">수신거부</a></p>
        </div>
    </div>
</body>
</html>""",
                userName, broadcasterName, broadcastTitle, broadcasterName,
                broadcastUrl, notificationProperties.getEmail().getFromName(), unsubscribeUrl
        );
    }

    /**
     * Kafka용 HTML 이메일 템플릿 생성
     */
    private String createKafkaEmailHtml(NotificationMessageDto messageDto) {
        String broadcastUrl = externalServiceProperties.getFrontendBaseUrl() + "/broadcast/" + messageDto.getBroadcastId();
        String unsubscribeUrl = externalServiceProperties.getUnsubscribeUrl();

        return String.format("""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; }
        .container { max-width: 600px; margin: 0 auto; background: white; }
        .header { background: #ff6b6b; color: white; padding: 20px; text-align: center; }
        .content { padding: 20px; }
        .button { 
            background: #ff6b6b; color: white; padding: 12px 24px; 
            text-decoration: none; border-radius: 5px; display: inline-block; 
        }
        .footer { padding: 20px; background: #f8f9fa; font-size: 12px; color: #666; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔴 라이브 방송 시작!</h1>
        </div>
        <div class="content">
            <h2>안녕하세요, %s님!</h2>
            <p><strong>%s</strong>님의 방송이 지금 시작되었습니다!</p>
            
            <h3>📺 방송 정보</h3>
            <ul>
                <li><strong>방송 제목:</strong> %s</li>
                <li><strong>방송자:</strong> %s</li>
                <li><strong>시작 시간:</strong> 지금!</li>
            </ul>
            
            <div style="margin: 20px 0;">
                <p>%s</p>
            </div>
            
            <p>지금 바로 시청하세요!</p>
            <a href="%s" class="button">🎬 방송 보러 가기</a>
        </div>
        <div class="footer">
            <p>%s | <a href="%s">수신거부</a></p>
        </div>
    </div>
</body>
</html>""",
                messageDto.getUserId(),
                messageDto.getBroadcasterName() != null ? messageDto.getBroadcasterName() : "방송자",
                messageDto.getTitle() != null ? messageDto.getTitle() : "방송",
                messageDto.getBroadcasterName() != null ? messageDto.getBroadcasterName() : "방송자",
                messageDto.getMessage() != null ? messageDto.getMessage() : "",
                broadcastUrl,
                notificationProperties.getEmail().getFromName(),
                unsubscribeUrl
        );
    }

    /**
     * 일반 알림용 HTML 템플릿 생성
     */
    private String createGeneralEmailHtml(NotificationMessageDto messageDto) {
        String unsubscribeUrl = externalServiceProperties.getUnsubscribeUrl();

        return String.format("""
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; }
        .container { max-width: 600px; margin: 0 auto; background: white; }
        .header { background: #007bff; color: white; padding: 20px; text-align: center; }
        .content { padding: 20px; }
        .footer { padding: 20px; background: #f8f9fa; font-size: 12px; color: #666; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>📢 알림</h1>
        </div>
        <div class="content">
            <h2>안녕하세요, %s님!</h2>
            <h3>%s</h3>
            <div style="margin: 20px 0; padding: 15px; background: #f8f9fa; border-radius: 5px;">
                <p>%s</p>
            </div>
        </div>
        <div class="footer">
            <p>%s | <a href="%s">수신거부</a></p>
        </div>
    </div>
</body>
</html>""",
                messageDto.getUserId(),
                messageDto.getTitle() != null ? messageDto.getTitle() : "알림",
                messageDto.getMessage() != null ? messageDto.getMessage() : "내용이 없습니다.",
                notificationProperties.getEmail().getFromName(),
                unsubscribeUrl
        );
    }

    /**
     * 이메일 마스킹 (로그용)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;

        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() <= 2) {
            return localPart + "@" + domain;
        }

        return localPart.substring(0, 2) + "***@" + domain;
    }
}
