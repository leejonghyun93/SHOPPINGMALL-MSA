package org.kosa.livestreamingservice.client.alarm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.kosa.livestreamingservice.repository.alarm.BroadcastRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 실제 운영용 방송 서비스 클라이언트
 * broadcaster_id를 통해 방송자 정보를 조회하고 UserServiceClient로 회원 정보 조회
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BroadcastServiceClient {

    private final BroadcastRepository broadcastRepository;
    private final UserServiceClient userServiceClient;

    /**
     * 지금 시작하는 방송 ID들 조회
     */
    public List<Long> getBroadcastsStartingNow() {
        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("방송 시작 체크: {}", now);

            // 현재 시간 ±1분 범위에서 시작하는 방송들 조회
            LocalDateTime startTime = now.minusMinutes(1);
            LocalDateTime endTime = now.plusMinutes(1);

            List<BroadcastEntity> startingBroadcasts =
                    broadcastRepository.findByScheduledStartTimeBetweenAndBroadcastStatus(
                            startTime, endTime, "scheduled");

            List<Long> broadcastIds = startingBroadcasts.stream()
                    .map(BroadcastEntity::getBroadcastId)
                    .collect(Collectors.toList());

            if (!broadcastIds.isEmpty()) {
                log.info("🎬 시작하는 방송들: {}", broadcastIds);
                startingBroadcasts.forEach(broadcast -> {
                    log.info("방송 정보: ID={}, 제목={}, broadcaster_id={}",
                            broadcast.getBroadcastId(),
                            broadcast.getTitle(),
                            broadcast.getBroadcasterId());
                });
            }

            return broadcastIds;

        } catch (Exception e) {
            log.error("방송 시작 목록 조회 실패: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 특정 방송 정보 조회 (방송자 정보 포함)
     */
    public BroadcastInfo getBroadcastInfo(Long broadcastId) {
        try {
            log.info("방송 정보 조회: broadcastId={}", broadcastId);

            // 1. 방송 정보 조회
            BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                    .orElse(null);

            if (broadcast == null) {
                log.warn("방송을 찾을 수 없습니다: broadcastId={}", broadcastId);
                return null;
            }

            // 2. broadcaster_id로 방송자 정보 조회 (String 타입)
            String broadcasterId = broadcast.getBroadcasterId();
            String broadcasterUserId = getBroadcasterUserId(broadcasterId);
            String broadcasterName = getBroadcasterName(broadcasterUserId, broadcasterId);

            BroadcastInfo broadcastInfo = BroadcastInfo.builder()
                    .broadcastId(broadcast.getBroadcastId())
                    .title(broadcast.getTitle())
                    .hostUserId(broadcasterUserId)
                    .broadcasterName(broadcasterName)
                    .scheduledStartTime(broadcast.getScheduledStartTime())
                    .broadcasterId(broadcasterId) // String으로 변경
                    .build();

            log.info("방송 정보 조회 성공: broadcastId={}, title={}, broadcaster_id={}, broadcasterName={}",
                    broadcastId, broadcastInfo.title, broadcasterId, broadcasterName);

            return broadcastInfo;

        } catch (Exception e) {
            log.error("방송 정보 조회 실패: broadcastId={}", broadcastId, e);
            return null;
        }
    }

    /**
     * broadcaster_id를 USER_ID로 변환
     * 실제 데이터베이스에서 broadcaster_id와 USER_ID 매핑
     */
    private String getBroadcasterUserId(String broadcasterId) {
        if (broadcasterId == null) {
            return null;
        }

        try {
            //  실제 운영: broadcaster_id를 그대로 USER_ID로 사용 (둘 다 String)
            String userIdCandidate = broadcasterId;

            // UserService에서 해당 USER_ID가 존재하는지 확인
            Map<String, Object> userInfo = userServiceClient.getUserInfo(userIdCandidate);
            if (userInfo != null && userInfo.get("name") != null) {
                log.info("broadcaster_id {}를 USER_ID {}로 매핑 성공", broadcasterId, userIdCandidate);
                return userIdCandidate;
            }

            log.warn("broadcaster_id {}에 해당하는 USER_ID를 찾을 수 없습니다", broadcasterId);
            return null;

        } catch (Exception e) {
            log.error("broadcaster_id {} 변환 실패: {}", broadcasterId, e.getMessage());
            return null;
        }
    }

    /**
     * 방송자 이름 조회
     */
    private String getBroadcasterName(String broadcasterUserId, String broadcasterId) {
        try {
            // 1. USER_ID가 있으면 UserService에서 실제 회원 이름 조회
            if (broadcasterUserId != null) {
                String name = userServiceClient.getUserName(broadcasterUserId);
                if (name != null && !name.trim().isEmpty()) {
                    log.info("방송자 이름 조회 성공: USER_ID={}, name={}", broadcasterUserId, name);
                    return name;
                }
            }

            // 2. 실제 회원 정보가 없으면 broadcaster_id 기반 기본 이름
            String defaultName = "방송자" + broadcasterId;
            log.warn("방송자 실제 이름을 찾을 수 없어 기본 이름 사용: USER_ID={}, defaultName={}",
                    broadcasterUserId, defaultName);
            return defaultName;

        } catch (Exception e) {
            log.error("방송자 이름 조회 실패: USER_ID={}, broadcaster_id={}, error={}",
                    broadcasterUserId, broadcasterId, e.getMessage());
            return "방송자" + broadcasterId;
        }
    }

    /**
     * 방송자 이름만 빠르게 조회
     */
    public String getBroadcasterName(Long broadcastId) {
        try {
            // BroadcastEntity를 조회해서 broadcaster_id(String) 가져오기
            BroadcastEntity broadcast = broadcastRepository.findById(broadcastId).orElse(null);
            if (broadcast == null) {
                return "알 수 없는 방송자";
            }

            String broadcasterId = broadcast.getBroadcasterId();
            String broadcasterUserId = getBroadcasterUserId(broadcasterId);
            return getBroadcasterName(broadcasterUserId, broadcasterId);

        } catch (Exception e) {
            log.error("방송자 이름 조회 실패: broadcastId={}", broadcastId, e);
            return "알 수 없는 방송자";
        }
    }

    /**
     * 특정 broadcaster_id의 방송 목록 조회
     */
    public List<BroadcastInfo> getBroadcastsByBroadcasterId(String broadcasterId) {
        try {
            List<BroadcastEntity> broadcasts =
                    broadcastRepository.findByBroadcasterIdOrderByScheduledStartTimeDesc(broadcasterId);

            String broadcasterUserId = getBroadcasterUserId(broadcasterId);
            String broadcasterName = getBroadcasterName(broadcasterUserId, broadcasterId);

            return broadcasts.stream()
                    .map(broadcast -> BroadcastInfo.builder()
                            .broadcastId(broadcast.getBroadcastId())
                            .title(broadcast.getTitle())
                            .hostUserId(broadcasterUserId)
                            .broadcasterName(broadcasterName)
                            .scheduledStartTime(broadcast.getScheduledStartTime())
                            .broadcasterId(broadcasterId) // String으로 변경
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("방송자별 방송 목록 조회 실패: broadcasterId={}", broadcasterId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 방송 존재 여부 확인
     */
    public boolean existsBroadcast(Long broadcastId) {
        try {
            return broadcastRepository.existsById(broadcastId);
        } catch (Exception e) {
            log.error("방송 존재 여부 확인 실패: broadcastId={}", broadcastId, e);
            return false;
        }
    }

    /**
     * 방송 상태 확인
     */
    public String getBroadcastStatus(Long broadcastId) {
        try {
            return broadcastRepository.findById(broadcastId)
                    .map(BroadcastEntity::getBroadcastStatus)
                    .orElse("unknown");
        } catch (Exception e) {
            log.error("방송 상태 확인 실패: broadcastId={}", broadcastId, e);
            return "unknown";
        }
    }

    /**
     * 테스트용 메서드 (개발 환경에서만 사용)
     */
    public List<Long> getBroadcastsStartingNowForTest() {
        List<Long> startingBroadcasts = new ArrayList<>();
        startingBroadcasts.add(11L);
        log.info("🧪 테스트용 방송 시작: {}", startingBroadcasts);
        return startingBroadcasts;
    }

    /**
     * 방송 정보 DTO
     */
    public static class BroadcastInfo {
        public Long broadcastId;
        public String title;
        public String hostUserId;        // 방송자의 USER_ID
        public String broadcasterName;   // 방송자의 이름
        public LocalDateTime scheduledStartTime;
        public String broadcasterId;     // 방송자 ID (문자열로 변경)

        public static BroadcastInfoBuilder builder() {
            return new BroadcastInfoBuilder();
        }

        public static class BroadcastInfoBuilder {
            private Long broadcastId;
            private String title;
            private String hostUserId;
            private String broadcasterName;
            private LocalDateTime scheduledStartTime;
            private String broadcasterId; // String으로 변경

            public BroadcastInfoBuilder broadcastId(Long broadcastId) {
                this.broadcastId = broadcastId;
                return this;
            }

            public BroadcastInfoBuilder title(String title) {
                this.title = title;
                return this;
            }

            public BroadcastInfoBuilder hostUserId(String hostUserId) {
                this.hostUserId = hostUserId;
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

            public BroadcastInfoBuilder broadcasterId(String broadcasterId) {
                this.broadcasterId = broadcasterId;
                return this;
            }

            public BroadcastInfo build() {
                BroadcastInfo info = new BroadcastInfo();
                info.broadcastId = this.broadcastId;
                info.title = this.title;
                info.hostUserId = this.hostUserId;
                info.broadcasterName = this.broadcasterName;
                info.scheduledStartTime = this.scheduledStartTime;
                info.broadcasterId = this.broadcasterId;
                return info;
            }
        }
    }
}