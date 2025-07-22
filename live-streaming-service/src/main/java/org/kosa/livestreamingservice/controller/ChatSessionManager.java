package org.kosa.livestreamingservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final MetricsController metricsController; // 추가

    // Redis 참여자 SET 키 생성
    private String getKey(Long broadcastId) {
        return "chat:participants:" + broadcastId;
    }

    // 세션 추가 시 호출
    public void addSession(Long broadcastId, String id, String sessionId) {
        String key = getKey(broadcastId);

        // 참여자 등록
        redisTemplate.opsForSet().add(key, id);

        // 세션ID → 참여자 ID, 방송ID 매핑 저장
        redisTemplate.opsForValue().set("chat:session:" + sessionId, id);
        redisTemplate.opsForValue().set("chat:session:broadcast:" + sessionId, String.valueOf(broadcastId));

        // TTL 설정
        redisTemplate.expire(key, 1, TimeUnit.HOURS);

        // 참여자 수 브로드캐스트
        broadcastCountToTopic(broadcastId);

        log.info("➕ 참여자 등록: ID={}, 방송ID={}, 세션ID={}", id, broadcastId, sessionId);
    }

    // 세션 제거 시 (sessionId 기준)
    public void removeSessionBySessionId(String sessionId) {
        String id = redisTemplate.opsForValue().get("chat:session:" + sessionId);
        String broadcastIdStr = redisTemplate.opsForValue().get("chat:session:broadcast:" + sessionId);

        if (id != null && broadcastIdStr != null) {
            Long broadcastId = Long.parseLong(broadcastIdStr);

            // 참여자 SET에서 제거
            redisTemplate.opsForSet().remove(getKey(broadcastId), id);
            broadcastCountToTopic(broadcastId);
            log.info("🧹 세션 제거: ID={}, 방송ID={}, 세션ID={}", id, broadcastId, sessionId);
        } else {
            log.warn("❌ 세션 정보 누락: sessionId={}", sessionId);
        }

        // 매핑 데이터 정리
        redisTemplate.delete("chat:session:" + sessionId);
        redisTemplate.delete("chat:session:broadcast:" + sessionId);
    }

    // REST API에서 직접 제거 요청 시 사용
    public void removeSessionManually(Long broadcastId, String id) {
        redisTemplate.opsForSet().remove(getKey(broadcastId), id);
        broadcastCountToTopic(broadcastId);
        log.info("🧹 수동 세션 제거 요청: ID={}, 방송ID={}", id, broadcastId);
    }

    // 현재 참여자 수 조회
    public int getParticipantCount(Long broadcastId) {
        Long count = redisTemplate.opsForSet().size(getKey(broadcastId));
        return count != null ? count.intValue() : 0;
    }

    // 참여자 수 STOMP로 전체 브로드캐스트 + 메트릭 업데이트
    public void broadcastCountToTopic(Long broadcastId) {
        int count = getParticipantCount(broadcastId);

        // 1. STOMP로 브로드캐스트
        messagingTemplate.convertAndSend("/topic/participants/" + broadcastId, count);

        // 2. 메트릭 업데이트 추가
        try {
            if (metricsController != null) {
                metricsController.updateChatParticipants(String.valueOf(broadcastId), count);
            }
        } catch (Exception e) {
            log.warn("메트릭 업데이트 실패: {}", e.getMessage());
        }

        log.debug("📊 참여자 수 브로드캐스트: 방송ID={}, 참여자수={}", broadcastId, count);
    }
}