package org.kosa.livestreamingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dao.chat.ChatDAO;
import org.kosa.livestreamingservice.dto.BroadcastDto;
import org.kosa.livestreamingservice.dto.BroadcastScheduleDto;
import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.kosa.livestreamingservice.repository.alarm.BroadcastRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BroadcastService {

    private final BroadcastRepository broadcastRepository;
    private final ChatDAO chatDAO;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 날짜별 방송 스케줄 조회
     */
    public List<BroadcastScheduleDto> getBroadcastScheduleByDate(LocalDate date) {
        try {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime nextDay = date.plusDays(1).atStartOfDay();

            List<BroadcastEntity> broadcasts = broadcastRepository.findBroadcastsByDateRange(
                    startOfDay, nextDay);

            // 시간대별로 그룹핑
            Map<String, List<BroadcastDto.AlarmDto>> groupedByTime = broadcasts.stream()
                    .collect(Collectors.groupingBy(
                            broadcast -> broadcast.getScheduledStartTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                            LinkedHashMap::new,
                            Collectors.mapping(this::convertToAlarmDto, Collectors.toList())
                    ));

            return groupedByTime.entrySet().stream()
                    .map(entry -> BroadcastScheduleDto.builder()
                            .time(entry.getKey())
                            .broadcasts(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("방송 스케줄 조회 중 오류 발생: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public void updateStatus(BroadcastEntity broadCast) {
        chatDAO.updateStatus(broadCast);
        messagingTemplate.convertAndSend("/topic/broadcast/" + broadCast.getBroadcastId() + "/status",
                Map.of("status", broadCast.getBroadcastStatus()));
    }

    /**
     * 방송 상세 조회
     */
    public BroadcastDto.DetailResponse getBroadcastDetail(Long broadcastId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        return convertToDetailResponseDto(broadcast);
    }

    /**
     * 방송자별 방송 목록 조회
     */
    public Page<BroadcastDto.Response> getBroadcastsByBroadcaster(String broadcasterId, Pageable pageable) {
        Page<BroadcastEntity> broadcasts = broadcastRepository.findByBroadcasterIdWithPaging(
                broadcasterId, pageable);

        return broadcasts.map(this::convertToResponseDto);
    }

    /**
     * 방송 일정 조회 (특정 날짜) - 프론트엔드 캘린더용
     */
    public List<BroadcastDto.ScheduleResponse> getBroadcastSchedule(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            List<BroadcastEntity> broadcasts = broadcastRepository.findBroadcastScheduleByDateRange(
                    startOfDay, endOfDay);

            Map<String, List<BroadcastDto.Response>> groupedByTime = broadcasts.stream()
                    .collect(Collectors.groupingBy(
                            broadcast -> {
                                if (broadcast.getScheduledStartTime() != null) {
                                    return broadcast.getScheduledStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                                } else if (broadcast.getActualStartTime() != null) {
                                    return broadcast.getActualStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
                                } else {
                                    return "시간 미정";
                                }
                            },
                            Collectors.mapping(this::convertToResponseDto, Collectors.toList())
                    ));

            return groupedByTime.entrySet().stream()
                    .map(entry -> BroadcastDto.ScheduleResponse.builder()
                            .time(entry.getKey())
                            .broadcasts(entry.getValue())
                            .build())
                    .sorted(Comparator.comparing(BroadcastDto.ScheduleResponse::getTime))
                    .collect(Collectors.toList());

        } catch (DateTimeParseException e) {
            log.error("잘못된 날짜 형식: {}", dateStr, e);
            throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다: " + dateStr);
        }
    }

    /**
     * 방송 통계 조회
     */
    public Map<String, Object> getBroadcastStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            long totalBroadcasts = broadcastRepository.count();
            long liveBroadcasts = broadcastRepository.countByBroadcastStatus("live");
            Long totalViewers = broadcastRepository.sumCurrentViewers();

            List<Object[]> categoryStats = broadcastRepository.findBroadcastCountByProductCategory();

            stats.put("totalBroadcasts", totalBroadcasts);
            stats.put("liveBroadcasts", liveBroadcasts);
            stats.put("totalViewers", totalViewers != null ? totalViewers : 0);
            stats.put("categoryStats", categoryStats);
            stats.put("timestamp", LocalDateTime.now());

            return stats;

        } catch (Exception e) {
            log.error("방송 통계 조회 중 오류 발생", e);
            throw new RuntimeException("방송 통계 조회에 실패했습니다", e);
        }
    }

    /**
     * 방송 시청자 수 업데이트
     */
    @Transactional
    public void updateViewerCount(Long broadcastId, Integer viewerCount) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        broadcast.setCurrentViewers(viewerCount);

        if (viewerCount > broadcast.getPeakViewers()) {
            broadcast.setPeakViewers(viewerCount);
        }

        broadcastRepository.save(broadcast);
    }

    /**
     * 방송 좋아요 수 업데이트
     */
    @Transactional
    public void updateLikeCount(Long broadcastId, Integer likeCount) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        broadcast.setLikeCount(likeCount);
        broadcastRepository.save(broadcast);
    }

    /**
     * 방송 검색
     */
    public List<BroadcastDto.Response> searchBroadcasts(String keyword, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<BroadcastEntity> broadcasts = broadcastRepository.findByTitleOrDescriptionContaining(keyword, pageable);

        return broadcasts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 추천 방송 목록 조회
     */
    public List<BroadcastDto.Response> getRecommendedBroadcasts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<BroadcastEntity> broadcasts = broadcastRepository.findRecommendedBroadcasts(pageable);

        return broadcasts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 라이브 방송 목록 조회 (통합된 카테고리 처리)
     */
    public List<BroadcastDto.Response> getLiveBroadcasts(String broadcastStatus, Integer isPublic, Integer categoryId, int limit) {
        try {
            String status = broadcastStatus != null ? broadcastStatus : "live";
            Boolean publicFilter = isPublic != null ? (isPublic == 1) : true;

            List<BroadcastEntity> broadcasts;

            if (categoryId != null) {
                // 카테고리별 조회 (통합된 메서드 사용)
                broadcasts = broadcastRepository.findLiveBroadcastsByCategoryNative(
                        status, publicFilter, categoryId, limit, 0);
            } else {
                // 전체 방송 조회
                broadcasts = broadcastRepository.findLiveBroadcastsForFrontendNative(
                        status, publicFilter, limit, 0);
            }

            List<BroadcastDto.Response> result = broadcasts.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());

            log.info("라이브 방송 조회 완료 - categoryId: {}, 결과 수: {}", categoryId, result.size());
            return result;

        } catch (Exception e) {
            log.error("라이브 방송 조회 중 오류 발생", e);
            throw new RuntimeException("방송 목록 조회에 실패했습니다", e);
        }
    }

    // ============ 변환 메서드들 ============

    /**
     * AlarmDto 변환
     */
    private BroadcastDto.AlarmDto convertToAlarmDto(BroadcastEntity entity) {
        return BroadcastDto.AlarmDto.builder()
                .id(entity.getBroadcastId())
                .title(entity.getTitle())
                .thumbnailUrl(entity.getThumbnailUrl())
                .broadcasterName(getBroadcasterName(entity.getBroadcasterId()))
                .productName(entity.getDescription())
                .salePrice(generateRandomPrice())
                .status(entity.getBroadcastStatus())
                .isNotificationSet(false)
                .build();
    }

    /**
     * Response DTO 변환
     */
    private BroadcastDto.Response convertToResponseDto(BroadcastEntity entity) {
        return BroadcastDto.Response.builder()
                .broadcastId(entity.getBroadcastId())
                .broadcasterId(entity.getBroadcasterId())
                .broadcasterName(getBroadcasterName(entity.getBroadcasterId()))
                .title(entity.getTitle())
                .description(entity.getDescription())
                .broadcastStatus(entity.getBroadcastStatus())
                .actualStartTime(entity.getActualStartTime())
                .currentViewers(entity.getCurrentViewers())
                .likeCount(entity.getLikeCount())
                .categoryId(entity.getCategoryId())
                .categoryName(getCategoryName(entity.getCategoryId()))
                .tags(entity.getTags())
                .thumbnailUrl(entity.getThumbnailUrl())
                .totalViewers(entity.getTotalViewers())
                .peakViewers(entity.getPeakViewers())
                .build();
    }

    /**
     * DetailResponse DTO 변환
     */
    private BroadcastDto.DetailResponse convertToDetailResponseDto(BroadcastEntity entity) {
        return BroadcastDto.DetailResponse.builder()
                .broadcastId(entity.getBroadcastId())
                .broadcasterId(entity.getBroadcasterId())
                .broadcasterName(getBroadcasterName(entity.getBroadcasterId()))
                .title(entity.getTitle())
                .description(entity.getDescription())
                .broadcastStatus(entity.getBroadcastStatus())
                .scheduledStartTime(entity.getScheduledStartTime())
                .scheduledEndTime(entity.getScheduledEndTime())
                .actualStartTime(entity.getActualStartTime())
                .actualEndTime(entity.getActualEndTime())
                .isPublic(entity.getIsPublic())
                .maxViewers(entity.getMaxViewers())
                .currentViewers(entity.getCurrentViewers())
                .totalViewers(entity.getTotalViewers())
                .peakViewers(entity.getPeakViewers())
                .likeCount(entity.getLikeCount())
                .thumbnailUrl(entity.getThumbnailUrl())
                .streamUrl(entity.getStreamUrl())
                .categoryId(entity.getCategoryId())
                .categoryName(getCategoryName(entity.getCategoryId()))
                .tags(entity.getTags())
                .streamKey(entity.getStreamKey())
                .videoUrl(entity.getVideoUrl())
                .obsHost(entity.getObsHost())
                .obsPort(entity.getObsPort())
                .nginxHost(entity.getNginxHost())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 방송자 이름 조회
     */
    private String getBroadcasterName(String broadcasterId) {
        if (broadcasterId == null || broadcasterId.trim().isEmpty()) {
            return "알 수 없는 방송자";
        }

        try {
            Optional<String> broadcasterName = broadcastRepository.findBroadcasterNameById(broadcasterId);
            return broadcasterName.orElse("방송자" + broadcasterId);
        } catch (Exception e) {
            log.warn("방송자 이름 조회 실패 - broadcasterId: {}", broadcasterId, e);
            return "방송자" + broadcasterId;
        }
    }

    /**
     * 카테고리 이름 조회
     */
    private String getCategoryName(Integer categoryId) {
        if (categoryId == null) return "일반";

        try {
            Optional<String> categoryName = broadcastRepository.findCategoryNameById(categoryId);
            return categoryName.orElse("카테고리" + categoryId);
        } catch (Exception e) {
            log.warn("카테고리 이름 조회 실패 - categoryId: {}", categoryId, e);
            return "카테고리" + categoryId;
        }
    }

    private Integer generateRandomPrice() {
        return (int) (Math.random() * 100000) + 10000;
    }
}