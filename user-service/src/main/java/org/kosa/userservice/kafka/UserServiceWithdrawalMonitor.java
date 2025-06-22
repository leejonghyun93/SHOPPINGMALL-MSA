package org.kosa.userservice.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.UserWithdrawalEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * User Service 내부에서 탈퇴 이벤트를 모니터링하는 컨슈머
 * (다른 서비스들이 제대로 처리하는지 확인용)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserServiceWithdrawalMonitor {

    @KafkaListener(topics = "${kafka.topic.user-withdrawal}", groupId = "user-service-monitor-group")
    public void monitorUserWithdrawal(UserWithdrawalEvent event) {
        try {
            log.info("=== 탈퇴 이벤트 모니터링 ===");
            log.info("사용자 ID: {}", event.getUserId());
            log.info("탈퇴 ID: {}", event.getWithdrawnId());
            log.info("이벤트 ID: {}", event.getEventId());
            log.info("탈퇴 사유: {}", event.getWithdrawalReason());
            log.info("탈퇴 처리일: {}", event.getWithdrawalDate());
            log.info("이벤트 발행 시간: {}", event.getEventTimestamp());
            log.info("========================");

        } catch (Exception e) {
            log.error("탈퇴 이벤트 모니터링 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}