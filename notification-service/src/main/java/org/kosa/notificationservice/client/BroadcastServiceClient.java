package org.kosa.notificationservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * 방송 서비스 클라이언트 (Live Streaming Service와 통신)
 */
@Component
@Slf4j
public class BroadcastServiceClient {

    /**
     * 지금 시작하는 방송 ID들 조회
     */
    public List<Long> getBroadcastsStartingNow() {
        try {
            // TODO: 실제 Live Streaming Service API 호출
            // RestTemplate이나 WebClient 사용

            // 임시로 더미 데이터 반환
            LocalDateTime now = LocalDateTime.now();
            log.info("방송 시작 체크: {}", now);

            // 테스트용 더미 방송 ID들
            List<Long> startingBroadcasts = new ArrayList<>();

            // 매분 0초에 방송 ID 1, 2, 3이 시작한다고 가정
            if (now.getSecond() >= 0 && now.getSecond() <= 10) {
                startingBroadcasts.add(1L);
                startingBroadcasts.add(2L);
                log.info("시작하는 방송들: {}", startingBroadcasts);
            }

            return startingBroadcasts;

        } catch (Exception e) {
            log.error("방송 시작 목록 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 특정 방송 정보 조회
     */
    public BroadcastInfo getBroadcastInfo(Long broadcastId) {
        try {
            // TODO: 실제 API 호출

            // 임시 더미 데이터
            return BroadcastInfo.builder()
                    .broadcastId(broadcastId)
                    .title("테스트 방송 " + broadcastId)
                    .broadcasterName("방송인" + broadcastId)
                    .scheduledStartTime(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("방송 정보 조회 실패: broadcastId={}", broadcastId, e);
            return null;
        }
    }

    /**
     * 방송 정보 DTO
     */
    public static class BroadcastInfo {
        public Long broadcastId;
        public String title;
        public String broadcasterName;
        public LocalDateTime scheduledStartTime;

        public static BroadcastInfoBuilder builder() {
            return new BroadcastInfoBuilder();
        }

        public static class BroadcastInfoBuilder {
            private Long broadcastId;
            private String title;
            private String broadcasterName;
            private LocalDateTime scheduledStartTime;

            public BroadcastInfoBuilder broadcastId(Long broadcastId) {
                this.broadcastId = broadcastId;
                return this;
            }

            public BroadcastInfoBuilder title(String title) {
                this.title = title;
                return this;
            }

            public BroadcastInfoBuilder broadcasterName(String broadcasterName) {
                this.broadcasterName = broadcasterName;
                return this;
            }

            public BroadcastInfoBuilder scheduledStartTime(LocalDateTime scheduledStartTime) {
                this.scheduledStartTime = scheduledStartTime;
                return this;
            }

            public BroadcastInfo build() {
                BroadcastInfo info = new BroadcastInfo();
                info.broadcastId = this.broadcastId;
                info.title = this.title;
                info.broadcasterName = this.broadcasterName;
                info.scheduledStartTime = this.scheduledStartTime;
                return info;
            }
        }
    }
}