package org.kosa.livestreamingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.BroadcastDto;
import org.kosa.livestreamingservice.dto.BroadcastScheduleDto;
import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.kosa.livestreamingservice.repository.alarm.BroadcastRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    // ============ 알림 서비스용 메소드 ============

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

            List<BroadcastScheduleDto> result = groupedByTime.entrySet().stream()
                    .map(entry -> BroadcastScheduleDto.builder()
                            .time(entry.getKey())
                            .broadcasts(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

            return result;

        } catch (Exception e) {
            log.error("방송 스케줄 조회 중 오류 발생: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ============ 관리자용 방송 상태 관리 메서드들 ============

    /**
     * 관리자용: 방송 시작 (scheduled/starting → live)
     */
    @Transactional
    public void adminStartBroadcast(Long broadcastId, String adminId) {
        log.info("관리자 방송 시작 요청: broadcastId={}, adminId={}", broadcastId, adminId);

        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        if (!Arrays.asList("schedule", "starting").contains(broadcast.getBroadcastStatus())) {
            throw new IllegalStateException("시작할 수 없는 방송 상태입니다: " + broadcast.getBroadcastStatus());
        }

        int updated = broadcastRepository.updateBroadcastToLive(broadcastId);

        if (updated > 0) {
            log.info("관리자 방송 시작 완료: broadcastId={}, adminId={}", broadcastId, adminId);
        } else {
            throw new RuntimeException("방송 시작에 실패했습니다");
        }
    }

    /**
     * 관리자용: 방송 종료 (live/paused/starting → ended)
     */
    @Transactional
    public void adminEndBroadcast(Long broadcastId, String adminId) {
        log.info("관리자 방송 종료 요청: broadcastId={}, adminId={}", broadcastId, adminId);

        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        if ("ended".equals(broadcast.getBroadcastStatus())) {
            throw new IllegalStateException("이미 종료된 방송입니다");
        }

        int updated = broadcastRepository.updateBroadcastToEnded(broadcastId);

        if (updated > 0) {
            log.info("관리자 방송 종료 완료: broadcastId={}, adminId={}", broadcastId, adminId);
        } else {
            throw new RuntimeException("방송 종료에 실패했습니다");
        }
    }

    /**
     * 관리자용: 방송 일시정지 (live → paused)
     */
    @Transactional
    public void adminPauseBroadcast(Long broadcastId, String adminId) {
        log.info("관리자 방송 일시정지 요청: broadcastId={}, adminId={}", broadcastId, adminId);

        int updated = broadcastRepository.updateBroadcastToPaused(broadcastId);

        if (updated > 0) {
            log.info("관리자 방송 일시정지 완료: broadcastId={}, adminId={}", broadcastId, adminId);
        } else {
            throw new RuntimeException("방송 일시정지에 실패했습니다. 현재 LIVE 상태인지 확인하세요.");
        }
    }

    /**
     * 관리자용: 방송 재개 (paused → live)
     */
    @Transactional
    public void adminResumeBroadcast(Long broadcastId, String adminId) {
        log.info("관리자 방송 재개 요청: broadcastId={}, adminId={}", broadcastId, adminId);

        int updated = broadcastRepository.updateBroadcastToResume(broadcastId);

        if (updated > 0) {
            log.info("관리자 방송 재개 완료: broadcastId={}, adminId={}", broadcastId, adminId);
        } else {
            throw new RuntimeException("방송 재개에 실패했습니다. 현재 PAUSED 상태인지 확인하세요.");
        }
    }

    /**
     * 관리자용: 방송 상태 직접 변경 (범용)
     */
    @Transactional
    public void adminChangeBroadcastStatus(Long broadcastId, String newStatus, String adminId) {
        log.info("관리자 방송 상태 변경 요청: broadcastId={}, newStatus={}, adminId={}",
                broadcastId, newStatus, adminId);

        List<String> validStatuses = Arrays.asList("schedule", "starting", "live", "paused", "ended");
        if (!validStatuses.contains(newStatus)) {
            throw new IllegalArgumentException("유효하지 않은 방송 상태입니다: " + newStatus);
        }

        int updated = broadcastRepository.updateBroadcastStatus(broadcastId, newStatus);

        if (updated > 0) {
            log.info("관리자 방송 상태 변경 완료: broadcastId={}, newStatus={}, adminId={}",
                    broadcastId, newStatus, adminId);
        } else {
            throw new RuntimeException("방송 상태 변경에 실패했습니다");
        }
    }

    /**
     * 관리자용: 여러 방송 일괄 종료
     */
    @Transactional
    public void adminEndMultipleBroadcasts(List<Long> broadcastIds, String adminId) {
        log.info("관리자 여러 방송 일괄 종료 요청: count={}, adminId={}", broadcastIds.size(), adminId);

        if (broadcastIds.isEmpty()) {
            throw new IllegalArgumentException("종료할 방송이 선택되지 않았습니다");
        }

        int updated = broadcastRepository.updateMultipleBroadcastsToEnded(broadcastIds);

        log.info("관리자 여러 방송 일괄 종료 완료: 요청={}, 처리={}, adminId={}",
                broadcastIds.size(), updated, adminId);
    }

    /**
     * 관리자용: 방송 상태별 통계 조회
     */
    public Map<String, Object> getAdminBroadcastStats() {
        Map<String, Object> stats = new HashMap<>();

        long scheduledCount = broadcastRepository.countByBroadcastStatus("schedule");
        long startingCount = broadcastRepository.countByBroadcastStatus("starting");
        long liveCount = broadcastRepository.countByBroadcastStatus("live");
        long pausedCount = broadcastRepository.countByBroadcastStatus("paused");
        long endedCount = broadcastRepository.countByBroadcastStatus("ended");

        stats.put("schedule", scheduledCount);
        stats.put("starting", startingCount);
        stats.put("live", liveCount);
        stats.put("paused", pausedCount);
        stats.put("ended", endedCount);
        stats.put("total", scheduledCount + startingCount + liveCount + pausedCount + endedCount);
        stats.put("timestamp", LocalDateTime.now());

        return stats;
    }

    // ============ 방송자용 메서드들 ============

    /**
     * 방송 시작 (방송자가 실제로 방송을 시작할 때)
     */
    @Transactional
    public void startBroadcast(Long broadcastId, String broadcasterId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        if (!broadcast.getBroadcasterId().equals(broadcasterId)) {
            throw new IllegalArgumentException("방송을 시작할 권한이 없습니다");
        }

        if (!Arrays.asList("schedule", "starting").contains(broadcast.getBroadcastStatus())) {
            throw new IllegalStateException("시작할 수 없는 방송 상태입니다: " + broadcast.getBroadcastStatus());
        }

        broadcast.setBroadcastStatus("live");
        broadcast.setActualStartTime(LocalDateTime.now());
        broadcast.setUpdatedAt(LocalDateTime.now());

        broadcastRepository.save(broadcast);
        log.info("방송 시작 완료: broadcastId={}, broadcasterId={}", broadcastId, broadcasterId);
    }

    /**
     * 방송 종료 (방송자가 방송을 종료할 때)
     */
    @Transactional
    public void endBroadcast(Long broadcastId, String broadcasterId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        if (!broadcast.getBroadcasterId().equals(broadcasterId)) {
            throw new IllegalArgumentException("방송을 종료할 권한이 없습니다");
        }

        if (!Arrays.asList("live", "paused", "starting").contains(broadcast.getBroadcastStatus())) {
            throw new IllegalStateException("종료할 수 없는 방송 상태입니다: " + broadcast.getBroadcastStatus());
        }

        broadcast.setBroadcastStatus("ended");
        broadcast.setActualEndTime(LocalDateTime.now());
        broadcast.setUpdatedAt(LocalDateTime.now());

        broadcastRepository.save(broadcast);
        log.info("방송 종료 완료: broadcastId={}, broadcasterId={}", broadcastId, broadcasterId);
    }

    /**
     * 방송 일시정지
     */
    @Transactional
    public void pauseBroadcast(Long broadcastId, String broadcasterId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        if (!broadcast.getBroadcasterId().equals(broadcasterId)) {
            throw new IllegalArgumentException("방송을 일시정지할 권한이 없습니다");
        }

        if (!"live".equals(broadcast.getBroadcastStatus())) {
            throw new IllegalStateException("일시정지할 수 없는 방송 상태입니다: " + broadcast.getBroadcastStatus());
        }

        broadcast.setBroadcastStatus("paused");
        broadcast.setUpdatedAt(LocalDateTime.now());
        broadcastRepository.save(broadcast);

        log.info("방송 일시정지: broadcastId={}", broadcastId);
    }

    /**
     * 방송 재개
     */
    @Transactional
    public void resumeBroadcast(Long broadcastId, String broadcasterId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        if (!broadcast.getBroadcasterId().equals(broadcasterId)) {
            throw new IllegalArgumentException("방송을 재개할 권한이 없습니다");
        }

        if (!"paused".equals(broadcast.getBroadcastStatus())) {
            throw new IllegalStateException("재개할 수 없는 방송 상태입니다: " + broadcast.getBroadcastStatus());
        }

        broadcast.setBroadcastStatus("live");
        broadcast.setUpdatedAt(LocalDateTime.now());
        broadcastRepository.save(broadcast);

        log.info("방송 재개: broadcastId={}", broadcastId);
    }

    /**
     * 현재 진행 중인 방송 목록 조회
     */
    public List<BroadcastDto.Response> getCurrentActiveBroadcasts() {
        List<BroadcastEntity> broadcasts = broadcastRepository.findCurrentActiveBroadcasts();

        return broadcasts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 특정 방송의 현재 상태 조회
     */
    public Map<String, Object> getBroadcastStatus(Long broadcastId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        Map<String, Object> status = new HashMap<>();
        status.put("broadcastId", broadcast.getBroadcastId());
        status.put("status", broadcast.getBroadcastStatus());
        status.put("scheduledStartTime", broadcast.getScheduledStartTime());
        status.put("actualStartTime", broadcast.getActualStartTime());
        status.put("actualEndTime", broadcast.getActualEndTime());
        status.put("currentViewers", broadcast.getCurrentViewers());
        status.put("isLive", "live".equals(broadcast.getBroadcastStatus()));
        status.put("isPaused", "paused".equals(broadcast.getBroadcastStatus()));
        status.put("isEnded", "ended".equals(broadcast.getBroadcastStatus()));
        status.put("updatedAt", broadcast.getUpdatedAt());

        return status;
    }

    // ============ 프론트엔드용 메소드들 ============

    /**
     * 진행 중인 라이브 방송 목록 조회 (프론트엔드용)
     */
    public List<BroadcastDto.Response> getLiveBroadcasts(String broadcastStatus, Integer isPublic, Integer categoryId, int limit) {
        try {
            String status = broadcastStatus != null ? broadcastStatus : "live";
            Boolean publicFilter = isPublic != null ? (isPublic == 1) : true;

            List<BroadcastDto.Response> result;

            if (categoryId != null) {
                result = getBroadcastsByProductCategoryImproved(categoryId, status, publicFilter, limit);
            } else {
                List<BroadcastEntity> broadcasts = broadcastRepository.findLiveBroadcastsForFrontendNative(
                        status, publicFilter, limit, 0);
                result = broadcasts.stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            }

            return result;

        } catch (Exception e) {
            log.error("라이브 방송 조회 중 오류 발생", e);
            throw new RuntimeException("방송 목록 조회에 실패했습니다", e);
        }
    }

    /**
     * 개선된 상품 카테고리 기반 방송 조회 (대분류/소분류 구분 처리)
     */
    private List<BroadcastDto.Response> getBroadcastsByProductCategoryImproved(Integer categoryId, String status, Boolean isPublic, int limit) {
        try {
            List<Object[]> subCategories = broadcastRepository.findSubCategoriesByParentId(categoryId);
            boolean isParentCategory = !subCategories.isEmpty();

            List<BroadcastEntity> broadcasts = new ArrayList<>();

            if (isParentCategory) {
                broadcasts = broadcastRepository.findLiveBroadcastsByParentCategoryNative(
                        status, isPublic, categoryId, limit, 0);
            } else {
                broadcasts = broadcastRepository.findLiveBroadcastsByProductCategoryForFrontendNative(
                        status, isPublic, categoryId, limit, 0);
            }

            return broadcasts.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("개선된 상품 카테고리 기반 조회 실패 - categoryId: {}", categoryId, e);

            try {
                List<BroadcastEntity> fallbackBroadcasts = broadcastRepository.findLiveBroadcastsByCategoryForFrontendNative(
                        status, isPublic, categoryId, limit, 0);
                return fallbackBroadcasts.stream()
                        .map(this::convertToResponseDto)
                        .collect(Collectors.toList());
            } catch (Exception fallbackError) {
                log.error("Fallback 조회도 실패 - categoryId: {}", categoryId, fallbackError);
                return new ArrayList<>();
            }
        }
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
     * 카테고리별 방송 목록 조회 (상품 카테고리 기준)
     */
    public List<BroadcastDto.Response> getBroadcastsByCategory(Integer categoryId, String broadcastStatus, int limit) {
        String status = broadcastStatus != null ? broadcastStatus : "live";
        return getBroadcastsByProductCategoryImproved(categoryId, status, true, limit);
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

    // ============ 디버깅용 메소드들 ============

    /**
     * 디버깅용 메소드 - 카테고리 데이터 확인
     */
    public Map<String, Object> debugCategoryData(Integer categoryId) {
        Map<String, Object> debugInfo = new HashMap<>();

        try {
            Optional<String> categoryName = broadcastRepository.findCategoryNameById(categoryId);
            debugInfo.put("selectedCategory", Map.of(
                    "categoryId", categoryId,
                    "categoryName", categoryName.orElse("없음")
            ));

            Integer productCount = broadcastRepository.countProductsByCategory(categoryId);
            debugInfo.put("productCount", productCount);

            Integer broadcastCount = broadcastRepository.countBroadcastsByProductCategory(categoryId);
            debugInfo.put("broadcastCount", broadcastCount);

            List<Object[]> sampleData = broadcastRepository.findSampleBroadcastProductMapping(categoryId);

            List<Map<String, Object>> sampleMappings = sampleData.stream()
                    .map(row -> {
                        Map<String, Object> mapping = new HashMap<>();
                        mapping.put("broadcastId", row[0]);
                        mapping.put("broadcastTitle", row[1]);
                        mapping.put("productId", row[2]);
                        mapping.put("productName", row[3]);
                        mapping.put("categoryId", row[4]);
                        mapping.put("categoryName", row[5]);
                        return mapping;
                    })
                    .collect(Collectors.toList());
            debugInfo.put("sampleMappings", sampleMappings);

            List<Object[]> subCategories = broadcastRepository.findSubCategoriesByParentId(categoryId);
            debugInfo.put("subCategories", subCategories.stream()
                    .map(row -> {
                        Map<String, Object> subCat = new HashMap<>();
                        subCat.put("categoryId", row[0]);
                        subCat.put("name", row[1]);
                        return subCat;
                    })
                    .collect(Collectors.toList()));

            if (!subCategories.isEmpty()) {
                Integer subCategoryBroadcastCount = broadcastRepository.countBroadcastsByParentCategory(categoryId);
                debugInfo.put("subCategoryBroadcastCount", subCategoryBroadcastCount);
            }

            return debugInfo;

        } catch (Exception e) {
            log.error("카테고리 디버깅 중 오류 발생", e);
            debugInfo.put("error", e.getMessage());
            return debugInfo;
        }
    }

    // ============ 변환 메소드들 ============

    /**
     * 개선된 시간 표시 로직
     */
    private String getDisplayTime(BroadcastEntity broadcast) {
        LocalDateTime displayTime = broadcast.getActualStartTime() != null ?
                broadcast.getActualStartTime() : broadcast.getScheduledStartTime();

        return displayTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * 개선된 AlarmDto 변환 (방송 상태 포함)
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
     * 프론트엔드용 Response DTO 변환
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
     * 프론트엔드용 DetailResponse DTO 변환
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
     * 방송자 이름 조회 (실제 DB에서 조회, 닉네임 우선)
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
     * 카테고리 이름 조회 (실제 상품의 카테고리 기반)
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