package org.kosa.livestreamingservice.service.alarm;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.config.alarm.NotificationProperties;
import org.kosa.livestreamingservice.dto.alarm.NotificationMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;

    @Value("${notification.email.from-email}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    @Value("${SPRING_MAIL_USERNAME:}")
    private String mailUsername;

    @Value("${SPRING_MAIL_HOST:smtp.gmail.com}")
    private String mailHost;

    @PostConstruct
    public void checkEmailConfig() {
        log.info("=== 이메일 설정 확인 ===");
        log.info("From Email: {}", fromEmail);
        log.info("From Name: {}", fromName);
        log.info("Mail Host: {}", mailHost);
        log.info("Mail Username: {}", maskEmail(mailUsername));
        log.info("Mail Sender: {}", mailSender.getClass().getSimpleName());

        // 환경변수 직접 확인
        String envFromEmail = System.getenv("PROD_NOTIFICATION_FROM_EMAIL");
        String envSpringMailFrom = System.getenv("SPRING_MAIL_FROM");
        String envNotificationFrom = System.getenv("NOTIFICATION_EMAIL_FROM_EMAIL");

        log.info("환경변수 PROD_NOTIFICATION_FROM_EMAIL: {}", envFromEmail);
        log.info("환경변수 SPRING_MAIL_FROM: {}", envSpringMailFrom);
        log.info("환경변수 NOTIFICATION_EMAIL_FROM_EMAIL: {}", envNotificationFrom);

        log.info("===============================");

        // From Email 검증
        if (fromEmail == null || fromEmail.trim().isEmpty() || fromEmail.startsWith("${")) {
            log.error("From Email이 올바르게 설정되지 않았습니다: {}", fromEmail);
            log.error("이메일 발송이 불가능합니다!");
        } else {
            log.info("From Email 설정 완료: {}", maskEmail(fromEmail));
        }
    }

    /**
     * 방송 시작 알림 이메일 발송
     */
    public void sendBroadcastStartNotification(NotificationMessageDto message, String userEmail) {
        try {
            log.info("방송 시작 알림 이메일 발송 시작");
            log.info("From Email = {}", fromEmail);
            log.info("To Email = {}", maskEmail(userEmail));
            log.info("방송 제목 = {}", message.getBroadcastTitle());

            // From Email 유효성 검사
            if (!isValidFromEmail(fromEmail)) {
                log.error("From Email이 설정되지 않았습니다: {}", fromEmail);
                return;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // 발신자 설정
            helper.setFrom(fromEmail, fromName);
            helper.setTo(userEmail);
            helper.setSubject(message.getBroadcastTitle() + " 방송이 시작되었습니다!");

            // HTML 내용 생성
            String htmlContent = createBroadcastStartEmailContent(message);
            helper.setText(htmlContent, true);

            // 이메일 발송
            mailSender.send(mimeMessage);

            log.info("방송 시작 알림 이메일 발송 성공: to={}", maskEmail(userEmail));

        } catch (Exception e) {
            log.error("방송 시작 알림 이메일 발송 실패: to={}, from={}, error={}",
                    maskEmail(userEmail), fromEmail, e.getMessage(), e);
        }
    }

    /**
     * 방송 시작 알림 이메일 발송 (단순 버전)
     */
    public void sendBroadcastStartNotification(String userEmail, String userId, String broadcastTitle,
                                               String broadcasterName, Long broadcastId) {
        try {
            log.info("방송 시작 알림 이메일 발송 (단순 버전)");
            log.info("From Email = {}", fromEmail);
            log.info("To Email = {}", maskEmail(userEmail));
            log.info("방송 제목 = {}", broadcastTitle);
            log.info("방송자 = {}", broadcasterName);

            if (!isValidFromEmail(fromEmail)) {
                log.error("From Email이 설정되지 않았습니다: {}", fromEmail);
                return;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userEmail);
            helper.setSubject(broadcastTitle + " 방송이 시작되었습니다!");

            String htmlContent = createSimpleBroadcastEmailContent(broadcastTitle, broadcasterName, broadcastId);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);

            log.info("방송 시작 알림 이메일 발송 성공 (단순): to={}, broadcast={}",
                    maskEmail(userEmail), broadcastTitle);

        } catch (Exception e) {
            log.error("방송 시작 알림 이메일 발송 실패 (단순): to={}, error={}",
                    maskEmail(userEmail), e.getMessage(), e);
        }
    }

    /**
     * 일반 알림 이메일 발송
     */
    public void sendGeneralNotification(NotificationMessageDto message, String userEmail) {
        try {
            log.info("일반 알림 이메일 발송: type={}", message.getType());

            if (!isValidFromEmail(fromEmail)) {
                log.error("From Email이 설정되지 않았습니다: {}", fromEmail);
                return;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userEmail);
            helper.setSubject(message.getTitle());
            helper.setText(message.getMessage());

            mailSender.send(mimeMessage);

            log.info("일반 알림 이메일 발송 성공: to={}", maskEmail(userEmail));

        } catch (Exception e) {
            log.error("일반 알림 이메일 발송 실패: to={}, error={}",
                    maskEmail(userEmail), e.getMessage(), e);
        }
    }

    /**
     * From Email 유효성 검사
     */
    private boolean isValidFromEmail(String email) {
        return email != null &&
                !email.trim().isEmpty() &&
                !email.startsWith("${") &&
                email.contains("@") &&
                !email.equals("noreply@shopmall.com");  // 기본값이 아닌지 확인
    }

    /**
     * 이메일 마스킹 (보안)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        if (localPart.length() <= 2) {
            return email;
        }
        return localPart.substring(0, 2) + "***@" + domain;
    }

    /**
     * 방송 시작 이메일 HTML 생성
     */
    private String createBroadcastStartEmailContent(NotificationMessageDto message) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(90deg, #667eea 0%%, #764ba2 100%%); 
                           color: white; padding: 20px; text-align: center;">
                    <h1>방송이 시작되었습니다!</h1>
                </div>
                
                <div style="padding: 20px; background-color: #f9f9f9;">
                    <h2 style="color: #333;">%s</h2>
                    <p style="font-size: 16px; color: #666;">
                        <strong>%s님</strong>의 라이브 방송이 지금 시작되었습니다!
                    </p>
                    
                    <div style="background: white; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <h3 style="margin-top: 0; color: #333;">방송 정보</h3>
                        <p><strong>방송 제목:</strong> %s</p>
                        <p><strong>방송자:</strong> %s</p>
                        <p><strong>시작 시간:</strong> %s</p>
                    </div>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="http://13.209.253.241/broadcast/%d" 
                           style="background: #667eea; color: white; padding: 15px 30px; 
                                  text-decoration: none; border-radius: 5px; display: inline-block;">
                            지금 시청하기
                        </a>
                    </div>
                </div>
                
                <div style="text-align: center; padding: 20px; color: #999; font-size: 12px;">
                    <p>이 이메일은 방송 알림 서비스에 의해 자동 발송되었습니다.</p>
                </div>
            </body>
            </html>
            """,
                message.getBroadcastTitle(),
                message.getBroadcasterName(),
                message.getBroadcastTitle(),
                message.getBroadcasterName(),
                message.getScheduledStartTime() != null ?
                        message.getScheduledStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "지금",
                message.getBroadcastId()
        );
    }

    /**
     * 단순 방송 시작 이메일 HTML 생성
     */
    private String createSimpleBroadcastEmailContent(String broadcastTitle, String broadcasterName, Long broadcastId) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background: linear-gradient(90deg, #667eea 0%%, #764ba2 100%%); 
                           color: white; padding: 20px; text-align: center;">
                    <h1>방송이 시작되었습니다!</h1>
                </div>
                
                <div style="padding: 20px; background-color: #f9f9f9;">
                    <h2 style="color: #333;">%s</h2>
                    <p style="font-size: 16px; color: #666;">
                        <strong>%s님</strong>의 라이브 방송이 지금 시작되었습니다!
                    </p>
                    
                    <div style="text-align: center; margin: 30px 0;">
                        <a href="http://13.209.253.241/broadcast/%d" 
                           style="background: #667eea; color: white; padding: 15px 30px; 
                                  text-decoration: none; border-radius: 5px; display: inline-block;">
                            지금 시청하기
                        </a>
                    </div>
                </div>
                
                <div style="text-align: center; padding: 20px; color: #999; font-size: 12px;">
                    <p>이 이메일은 방송 알림 서비스에 의해 자동 발송되었습니다.</p>
                </div>
            </body>
            </html>
            """,
                broadcastTitle,
                broadcasterName,
                broadcastId
        );
    }
}