package org.kosa.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 📧 방송 시작 알림 서비스
 *
 * 주요 기능:
 * 1. 방송 시작 알림 구독/취소
 * 2. 배치를 통한 자동 알림 발송 (1분마다)
 * 3. 카프카를 통한 이메일 발송
 * 4. 실시간 WebSocket 알림 (선택)
 */
@SpringBootApplication
@EnableKafka            // 카프카 메시징 활성화
@EnableScheduling       // 스케줄링 활성화 (배치 작업용)
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);

        System.out.println("🔴=================================🔴");
        System.out.println("📧 방송 시작 알림 서비스 시작!");
        System.out.println("🔥 배치 작업: 1분마다 방송 시작 체크");
        System.out.println("📨 이메일 발송: 카프카 메시징");
        System.out.println("🔴=================================🔴");
    }
}