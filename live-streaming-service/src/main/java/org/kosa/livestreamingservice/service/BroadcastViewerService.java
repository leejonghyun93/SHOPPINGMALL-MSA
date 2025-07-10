package org.kosa.livestreamingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.ViewerResponse; // 🔥 별도 파일 import
import org.kosa.livestreamingservice.dto.ProductDto;
import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.kosa.livestreamingservice.repository.alarm.BroadcastRepository;
import org.kosa.livestreamingservice.repository.BroadcastViewerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BroadcastViewerService {

    private final BroadcastRepository broadcastRepository;
    private final BroadcastViewerRepository broadcastViewerRepository;

    /**
     * 방송 상세 정보 조회 -  별도 ViewerResponse 사용
     */
    public ViewerResponse getBroadcastDetail(Long broadcastId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        // 방송자 정보 조회
        String broadcasterName = getBroadcasterName(broadcast.getBroadcasterId());
        String categoryName = getCategoryName(broadcast.getCategoryId());

        log.info("🎥 방송 상세 조회 - broadcastId: {}, streamUrl: {}",
                broadcastId, broadcast.getStreamUrl());

        return ViewerResponse.builder() // 🔥 BroadcastDto. 제거
                .broadcastId(broadcast.getBroadcastId())
                .broadcasterId(broadcast.getBroadcasterId())
                .broadcasterName(broadcasterName)
                .title(broadcast.getTitle())
                .description(broadcast.getDescription())
                .broadcastStatus(broadcast.getBroadcastStatus())
                .scheduledStartTime(broadcast.getScheduledStartTime())
                .actualStartTime(broadcast.getActualStartTime())
                .currentViewers(broadcast.getCurrentViewers())
                .totalViewers(broadcast.getTotalViewers())
                .peakViewers(broadcast.getPeakViewers())
                .likeCount(broadcast.getLikeCount())
                .thumbnailUrl(broadcast.getThumbnailUrl())
                .streamUrl(broadcast.getStreamUrl()) //  DB에서 그대로 사용
                .categoryId(broadcast.getCategoryId())
                .categoryName(categoryName)
                .tags(broadcast.getTags())
                .isPublic(broadcast.getIsPublic())
                .createdAt(broadcast.getCreatedAt())
                // 스트림 정보도 DB에서 그대로
                .streamKey(broadcast.getStreamKey())
                .nginxHost(broadcast.getNginxHost())
                .obsHost(broadcast.getObsHost())
                .obsPort(broadcast.getObsPort())
                .build();
    }

    // 나머지 메소드들은 동일...

    public List<ProductDto.BroadcastProduct> getBroadcastProducts(Long broadcastId) {
        List<Object[]> productResults = broadcastViewerRepository.findBroadcastProductsWithDetails(broadcastId);

        return productResults.stream()
                .map(this::convertToProductDto)
                .sorted(Comparator.comparing(ProductDto.BroadcastProduct::getDisplayOrder))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Transactional
    public Map<String, Object> increaseViewerCount(Long broadcastId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        Integer currentViewers = broadcast.getCurrentViewers() + 1;
        broadcast.setCurrentViewers(currentViewers);

        if (currentViewers > broadcast.getPeakViewers()) {
            broadcast.setPeakViewers(currentViewers);
        }

        broadcast.setTotalViewers(broadcast.getTotalViewers() + 1);
        broadcastRepository.save(broadcast);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("currentViewers", currentViewers);
        result.put("totalViewers", broadcast.getTotalViewers());
        result.put("peakViewers", broadcast.getPeakViewers());

        return result;
    }

    @Transactional
    public Map<String, Object> likeBroadcast(Long broadcastId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        Integer likeCount = broadcast.getLikeCount() + 1;
        broadcast.setLikeCount(likeCount);
        broadcastRepository.save(broadcast);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("likeCount", likeCount);

        return result;
    }

    public Map<String, Object> getBroadcastStatus(Long broadcastId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        Map<String, Object> status = new HashMap<>();
        status.put("broadcastId", broadcast.getBroadcastId());
        status.put("broadcastStatus", broadcast.getBroadcastStatus());
        status.put("currentViewers", broadcast.getCurrentViewers());
        status.put("likeCount", broadcast.getLikeCount());
        status.put("isLive", "live".equals(broadcast.getBroadcastStatus()));
        status.put("streamUrl", broadcast.getStreamUrl());
        status.put("timestamp", LocalDateTime.now());

        return status;
    }

    private ProductDto.BroadcastProduct convertToProductDto(Object[] row) {
        return ProductDto.BroadcastProduct.builder()
                .productId((Integer) row[0])
                .name((String) row[1])
                .price((Integer) row[2])
                .salePrice((Integer) row[3])
                .productDescription((String) row[4])
                .mainImage((String) row[5])
                .categoryId((Integer) row[6])
                .categoryName((String) row[7])
                .displayOrder((Integer) row[8])
                .specialPrice((Integer) row[9])
                .isFeatured((Boolean) row[10])
                .stock((Integer) row[11])
                .productRating(row[12] != null ? row[12].toString() : "0.0")
                .viewCount((Integer) row[13])
                .build();
    }

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
}