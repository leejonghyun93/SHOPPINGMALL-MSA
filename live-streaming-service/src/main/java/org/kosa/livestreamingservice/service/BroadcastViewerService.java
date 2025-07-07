package org.kosa.livestreamingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.livestreamingservice.dto.BroadcastDto;
import org.kosa.livestreamingservice.dto.ProductDto;
import org.kosa.livestreamingservice.entity.BroadcastEntity;
import org.kosa.livestreamingservice.repository.alarm.BroadcastRepository;
import org.kosa.livestreamingservice.repository.BroadcastViewerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BroadcastViewerService {

    private final BroadcastRepository broadcastRepository;
    private final BroadcastViewerRepository broadcastViewerRepository;

    /**
     * 방송 상세 정보 조회
     */
    public BroadcastDto.ViewerResponse getBroadcastDetail(Long broadcastId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        // 방송자 정보 조회
        String broadcasterName = getBroadcasterName(broadcast.getBroadcasterId());
        String categoryName = getCategoryName(broadcast.getCategoryId());

        return BroadcastDto.ViewerResponse.builder()
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
                .streamUrl(broadcast.getStreamUrl())
                .categoryId(broadcast.getCategoryId())
                .categoryName(categoryName)
                .tags(broadcast.getTags())
                .isPublic(broadcast.getIsPublic())
                .createdAt(broadcast.getCreatedAt())
                .build();
    }

    /**
     * 방송의 상품 목록 조회
     */
    public List<ProductDto.BroadcastProduct> getBroadcastProducts(Long broadcastId) {
        List<Object[]> productResults = broadcastViewerRepository.findBroadcastProductsWithDetails(broadcastId);

        return productResults.stream()
                .map(this::convertToProductDto)
                .sorted(Comparator.comparing(ProductDto.BroadcastProduct::getDisplayOrder))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 방송 시청자 수 증가
     */
    @Transactional
    public Map<String, Object> increaseViewerCount(Long broadcastId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        // 현재 시청자 수 증가
        Integer currentViewers = broadcast.getCurrentViewers() + 1;
        broadcast.setCurrentViewers(currentViewers);

        // 최대 시청자 수 업데이트
        if (currentViewers > broadcast.getPeakViewers()) {
            broadcast.setPeakViewers(currentViewers);
        }

        // 총 시청자 수 업데이트
        broadcast.setTotalViewers(broadcast.getTotalViewers() + 1);

        broadcastRepository.save(broadcast);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("currentViewers", currentViewers);
        result.put("totalViewers", broadcast.getTotalViewers());
        result.put("peakViewers", broadcast.getPeakViewers());

        return result;
    }

    /**
     * 방송 좋아요
     */
    @Transactional
    public Map<String, Object> likeBroadcast(Long broadcastId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        // 좋아요 수 증가
        Integer likeCount = broadcast.getLikeCount() + 1;
        broadcast.setLikeCount(likeCount);

        broadcastRepository.save(broadcast);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("likeCount", likeCount);

        return result;
    }

    /**
     * 방송 상태 확인 (실시간 업데이트용)
     */
    public Map<String, Object> getBroadcastStatus(Long broadcastId) {
        BroadcastEntity broadcast = broadcastRepository.findById(broadcastId)
                .orElseThrow(() -> new IllegalArgumentException("방송을 찾을 수 없습니다: " + broadcastId));

        Map<String, Object> status = new HashMap<>();
        status.put("broadcastId", broadcast.getBroadcastId());
        status.put("broadcastStatus", broadcast.getBroadcastStatus());
        status.put("currentViewers", broadcast.getCurrentViewers());
        status.put("likeCount", broadcast.getLikeCount());
        status.put("isLive", "live".equals(broadcast.getBroadcastStatus()));
        status.put("timestamp", LocalDateTime.now());

        return status;
    }

    /**
     * 채팅 메시지 조회 (Mock 데이터)
     */
    public List<Map<String, Object>> getChatMessages(Long broadcastId, int page, int size) {
        // 실제 구현에서는 채팅 DB에서 조회
        List<Map<String, Object>> messages = new ArrayList<>();

        // Mock 데이터 생성
        String[] usernames = {"구매고민중", "냉장고맘", "가전왕", "절약이", "리뷰어", "방송팬", "쇼핑러버"};
        String[] messageTexts = {
                "안녕하세요!",
                "이 제품 어떤가요?",
                "가격이 궁금해요",
                "배송은 언제 되나요?",
                "할인 더 있나요?",
                "색상 다른 것도 있나요?",
                "설치비는 별도인가요?",
                "환불 가능한가요?",
                "좋은 정보 감사합니다",
                "주문하고 싶어요"
        };

        for (int i = 0; i < size && i < 20; i++) {
            Map<String, Object> message = new HashMap<>();
            message.put("id", (page * size) + i + 1);
            message.put("username", usernames[i % usernames.length]);
            message.put("message", messageTexts[i % messageTexts.length]);
            message.put("timestamp", LocalDateTime.now().minusMinutes(i).format(DateTimeFormatter.ofPattern("HH:mm")));
            message.put("isMine", false);
            messages.add(message);
        }

        return messages;
    }

    /**
     * 채팅 메시지 전송 (Mock)
     */
    @Transactional
    public Map<String, Object> sendChatMessage(Long broadcastId, Map<String, Object> messageData) {
        // 실제 구현에서는 채팅 DB에 저장하고 WebSocket으로 브로드캐스트

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("messageId", System.currentTimeMillis());
        result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        result.put("message", "메시지가 전송되었습니다");

        return result;
    }

    // ============ 유틸리티 메소드들 ============

    /**
     * Object[] 배열을 ProductDto로 변환
     */
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
}