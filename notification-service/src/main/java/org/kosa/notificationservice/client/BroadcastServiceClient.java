package org.kosa.notificationservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.notificationservice.repository.BroadcastRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 방송 서비스 클라이언트 (실제 데이터베이스 연동)
 * tb_live_broadcasts와 tb_member 테이블을 JOIN하여 실제 방송자 정보 조회
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BroadcastServiceClient {

    private final BroadcastRepository broadcastRepository;
    private final UserServiceClient userServiceClient;

    /**
     * 지금 시작하는 방송 ID들 조회 (실제 DB에서)
     */
    public List<Long> getBroadcastsStartingNow() {
        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("방송 시작 체크: {}", now);

            // 현재 시간 ±1분 범위에서 시작하는 방송들 조회
            LocalDateTime startTime = now.minusMinutes(1);
            LocalDateTime endTime = now.plusMinutes(1);

            List<BroadcastRepository.BroadcastWithMemberInfo> startingBroadcasts =
                    broadcastRepository.findStartingBroadcastsWithMemberInfo(startTime, endTime, "scheduled");

            List<Long> broadcastIds = startingBroadcasts.stream()
                    .map(BroadcastRepository.BroadcastWithMemberInfo::getBroadcastId)
                    .collect(Collectors.toList());

            if (!broadcastIds.isEmpty()) {
                startingBroadcasts.forEach(broadcast -> {
                    log.info("🎬 시작하는 방송: ID={}, 제목={}, 방송자={} ({})",
                            broadcast.getBroadcastId(),
                            broadcast.getTitle(),
                            broadcast.getBroadcasterName(),
                            broadcast.getBroadcasterUserId());
                });
            }

            return broadcastIds;

        } catch (Exception e) {
            log.error("방송 시작 목록 조회 실패: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * 특정 방송 정보 조회 (실제 방송자 정보 포함)
     */
    public BroadcastInfo getBroadcastInfo(Long broadcastId) {
        try {
            log.info("방송 정보 조회: broadcastId={}", broadcastId);

            // 🔥 실제 DB에서 방송 정보와 방송자 정보를 함께 조회
            BroadcastRepository.BroadcastWithMemberInfo broadcastWithMember =
                    broadcastRepository.findBroadcastWithMemberInfo(broadcastId)
                            .orElse(null);

            if (broadcastWithMember == null) {
                log.warn("방송을 찾을 수 없습니다: broadcastId={}", broadcastId);
                return null;
            }

            // 방송자 이름이 없는 경우 USER_ID로 대체
            String broadcasterName = broadcastWithMember.getBroadcasterName() != null ?
                    broadcastWithMember.getBroadcasterName() :
                    "사용자" + broadcastWithMember.getBroadcasterUserId();

            BroadcastInfo broadcastInfo = BroadcastInfo.builder()
                    .broadcastId(broadcastWithMember.getBroadcastId())
                    .title(broadcastWithMember.getTitle())
                    .hostUserId(broadcastWithMember.getBroadcasterUserId())
                    .broadcasterName(broadcasterName)
                    .scheduledStartTime(broadcastWithMember.getScheduledStartTime())
                    .build();

            log.info("방송 정보 조회 성공: broadcastId={}, title={}, broadcasterName={}, hostUserId={}",
                    broadcastId, broadcastInfo.title, broadcastInfo.broadcasterName, broadcastInfo.hostUserId);

            return broadcastInfo;

        } catch (Exception e) {
            log.error("방송 정보 조회 실패: broadcastId={}", broadcastId, e);
            return null;
        }
    }

    /**
     * 🔥 방송자 이름만 빠르게 조회
     */
    public String getBroadcasterName(Long broadcastId) {
        try {
            return broadcastRepository.findBroadcasterNameByBroadcastId(broadcastId)
                    .orElse("알 수 없는 방송자");
        } catch (Exception e) {
            log.error("방송자 이름 조회 실패: broadcastId={}", broadcastId, e);
            return "알 수 없는 방송자";
        }
    }

    /**
     * 🔥 방송자 USER_ID만 빠르게 조회
     */
    public String getBroadcasterUserId(Long broadcastId) {
        try {
            return broadcastRepository.findBroadcasterUserIdByBroadcastId(broadcastId)
                    .orElse(null);
        } catch (Exception e) {
            log.error("방송자 USER_ID 조회 실패: broadcastId={}", broadcastId, e);
            return null;
        }
    }

    /**
     * 🔥 특정 방송자의 방송 목록 조회
     */
    public List<BroadcastInfo> getBroadcastsByBroadcaster(String broadcasterUserId) {
        try {
            List<BroadcastRepository.BroadcastWithMemberInfo> broadcasts =
                    broadcastRepository.findBroadcastsByBroadcasterUserId(broadcasterUserId);

            return broadcasts.stream()
                    .map(broadcast -> BroadcastInfo.builder()
                            .broadcastId(broadcast.getBroadcastId())
                            .title(broadcast.getTitle())
                            .hostUserId(broadcast.getBroadcasterUserId())
                            .broadcasterName(broadcast.getBroadcasterName())
                            .scheduledStartTime(broadcast.getScheduledStartTime())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("방송자별 방송 목록 조회 실패: broadcasterUserId={}", broadcasterUserId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 🔥 테스트용: 항상 방송이 시작하는 메서드 (개발/테스트시에만 사용)
     */
    public List<Long> getBroadcastsStartingNowForTest() {
        List<Long> startingBroadcasts = new ArrayList<>();
        startingBroadcasts.add(11L);  // 테스트용 방송
        log.info("🧪 테스트용 방송 시작: {}", startingBroadcasts);
        return startingBroadcasts;
    }

    /**
     * 방송 정보 DTO
     */
    public static class BroadcastInfo {
        public Long broadcastId;
        public String title;
        public String hostUserId;        // 방송자의 USER_ID (tb_member.USER_ID)
        public String broadcasterName;   // 방송자의 실제 이름 (tb_member.NAME)
        public LocalDateTime scheduledStartTime;

        public static BroadcastInfoBuilder builder() {
            return new BroadcastInfoBuilder();
        }

        public static class BroadcastInfoBuilder {
            private Long broadcastId;
            private String title;
            private String hostUserId;
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

            public BroadcastInfo build() {
                BroadcastInfo info = new BroadcastInfo();
                info.broadcastId = this.broadcastId;
                info.title = this.title;
                info.hostUserId = this.hostUserId;
                info.broadcasterName = this.broadcasterName;
                info.scheduledStartTime = this.scheduledStartTime;
                return info;
            }
        }
    }
}