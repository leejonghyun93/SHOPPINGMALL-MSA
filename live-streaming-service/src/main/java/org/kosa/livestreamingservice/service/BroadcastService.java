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
import org.springframework.data.domain.Sort;
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

    // ============ 기존 알림 서비스용 메소드 ============

    /**
     * 날짜별 방송 스케줄 조회 (기존)
     */
    public List<BroadcastScheduleDto> getBroadcastScheduleByDate(LocalDate date) {
        List<BroadcastEntity> broadcasts = broadcastRepository.findByScheduledDate(date);

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
    }

    // ============ 프론트엔드용 새로운 메소드들 ============

    /**
     * 진행 중인 라이브 방송 목록 조회 (프론트엔드용) - 상품 카테고리 기반 필터링
     */
    public List<BroadcastDto.Response> getLiveBroadcasts(String broadcastStatus, Integer isPublic, Integer categoryId, int limit) {
        try {
            // 기본값 설정
            String status = broadcastStatus != null ? broadcastStatus : "live";
            Boolean publicFilter = isPublic != null ? (isPublic == 1) : true;

            List<BroadcastDto.Response> result;

            if (categoryId != null) {
                // 상품 카테고리 기준으로 필터링 - 개선된 로직 사용
                result = getBroadcastsByProductCategoryImproved(categoryId, status, publicFilter, limit);
            } else {
                // 전체 조회 - Native Query 사용
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
            // 먼저 해당 카테고리에 하위 카테고리가 있는지 확인
            List<Object[]> subCategories = broadcastRepository.findSubCategoriesByParentId(categoryId);
            boolean isParentCategory = !subCategories.isEmpty();

            List<BroadcastEntity> broadcasts = new ArrayList<>();

            if (isParentCategory) {
                // 대분류인 경우: 하위 카테고리의 상품들이 포함된 방송 조회
                broadcasts = broadcastRepository.findLiveBroadcastsByParentCategoryNative(
                        status, isPublic, categoryId, limit, 0);
            } else {
                // 소분류인 경우: 직접 해당 카테고리의 상품이 포함된 방송 조회
                broadcasts = broadcastRepository.findLiveBroadcastsByProductCategoryForFrontendNative(
                        status, isPublic, categoryId, limit, 0);
            }

            return broadcasts.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("개선된 상품 카테고리 기반 조회 실패 - categoryId: {}", categoryId, e);

            // fallback: 기본 조회 (방송 자체의 카테고리로 필터링)
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
     * 상품 카테고리 기반 방송 조회 (Native Query 사용) - 호환성 유지
     */
    private List<BroadcastDto.Response> getBroadcastsByProductCategoryNative(Integer categoryId, String status, Boolean isPublic, int limit) {
        try {
            // Native Query를 사용하여 상품 카테고리 기준 방송 조회
            List<BroadcastEntity> broadcasts = broadcastRepository.findLiveBroadcastsByProductCategoryForFrontendNative(
                    status, isPublic, categoryId, limit, 0);

            return broadcasts.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("상품 카테고리 기반 조회 실패 (Native Query), 기본 조회로 fallback - categoryId: {}", categoryId, e);

            // fallback: 기본 조회 (방송 자체의 카테고리로 필터링)
            List<BroadcastEntity> fallbackBroadcasts = broadcastRepository.findLiveBroadcastsByCategoryForFrontendNative(
                    status, isPublic, categoryId, limit, 0);

            return fallbackBroadcasts.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
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
     * 카테고리별 방송 목록 조회 (상품 카테고리 기준) - 개선된 로직 사용
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

            // 시간별로 그룹핑
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
            // 전체 통계
            long totalBroadcasts = broadcastRepository.count();
            long liveBroadcasts = broadcastRepository.countByBroadcastStatus("live");
            Long totalViewers = broadcastRepository.sumCurrentViewers();

            // 카테고리별 통계 (상품 카테고리 기준)
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

        // 최대 시청자 수 업데이트
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
            // 1. 선택된 카테고리 정보 확인
            Optional<String> categoryName = broadcastRepository.findCategoryNameById(categoryId);
            debugInfo.put("selectedCategory", Map.of(
                    "categoryId", categoryId,
                    "categoryName", categoryName.orElse("없음")
            ));

            // 2. 해당 카테고리의 상품 개수 확인
            Integer productCount = broadcastRepository.countProductsByCategory(categoryId);
            debugInfo.put("productCount", productCount);

            // 3. 해당 카테고리 상품이 포함된 방송 개수 확인
            Integer broadcastCount = broadcastRepository.countBroadcastsByProductCategory(categoryId);
            debugInfo.put("broadcastCount", broadcastCount);

            // 4. 실제 방송-상품-카테고리 매핑 데이터 확인
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

            // 5. 하위 카테고리가 있는지 확인
            List<Object[]> subCategories = broadcastRepository.findSubCategoriesByParentId(categoryId);
            debugInfo.put("subCategories", subCategories.stream()
                    .map(row -> {
                        Map<String, Object> subCat = new HashMap<>();
                        subCat.put("categoryId", row[0]);
                        subCat.put("name", row[1]);
                        return subCat;
                    })
                    .collect(Collectors.toList()));

            // 6. 만약 대분류라면 하위 카테고리들의 상품이 포함된 방송도 확인
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
     * 기존 DTO 변환 (알림 서비스용)
     */
    private BroadcastDto.AlarmDto convertToAlarmDto(BroadcastEntity entity) {
        return BroadcastDto.AlarmDto.builder()
                .id(entity.getBroadcastId())
                .title(entity.getTitle())
                .thumbnailUrl(entity.getThumbnailUrl())
                .broadcasterName("방송자" + entity.getBroadcasterId())
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
     * 카테고리명이 이미 조회된 경우 사용하는 변환 메소드
     */
    private BroadcastDto.Response convertToResponseDtoWithCategory(BroadcastEntity entity, String categoryName) {
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
                .categoryName(categoryName) // 이미 조회된 카테고리명 사용
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
            // 실제 카테고리 정보를 조회하는 쿼리 실행
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