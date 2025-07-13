package org.kosa.commerceservice.service.product;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.commerceservice.dto.product.WishDTO;
import org.kosa.commerceservice.entity.product.Wish;
import org.kosa.commerceservice.repository.product.WishRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WishService {

    private final WishRepository wishRepository;

    /**
     * 찜하기 추가
     */
    @Transactional
    public boolean addToWishlist(String userId, Integer productId) {
        try {
            // 이미 찜한 상품인지 확인
            Optional<Wish> existingWish = wishRepository.findByUserIdAndProductId(userId, productId);
            if (existingWish.isPresent()) {
                return false; // 이미 찜한 상품
            }

            // 새로운 찜하기 생성
            Wish wish = Wish.builder()
                    .userId(userId)
                    .productId(productId)
                    .createdDate(LocalDateTime.now())
                    .build();

            wishRepository.save(wish);
            return true;
        } catch (Exception e) {
            log.error("찜하기 추가 실패 - userId: {}, productId: {}", userId, productId, e);
            return false;
        }
    }

    /**
     * 찜하기 해제
     */
    @Transactional
    public boolean removeFromWishlist(String userId, Integer productId) {
        try {
            Optional<Wish> wish = wishRepository.findByUserIdAndProductId(userId, productId);
            if (wish.isPresent()) {
                wishRepository.delete(wish.get());
                return true;
            }
            return false; // 찜하지 않은 상품
        } catch (Exception e) {
            log.error("찜하기 해제 실패 - userId: {}, productId: {}", userId, productId, e);
            return false;
        }
    }

    /**
     * 찜하기 상태 확인
     */
    public boolean isWishlisted(String userId, Integer productId) {
        try {
            return wishRepository.findByUserIdAndProductId(userId, productId).isPresent();
        } catch (Exception e) {
            log.error("찜하기 상태 확인 실패 - userId: {}, productId: {}", userId, productId, e);
            return false;
        }
    }

    /**
     * 사용자의 찜한 상품 목록 조회 (상품 정보 포함)
     */
    public List<WishDTO> getUserWishlist(String userId) {
        try {
            List<Object[]> results = wishRepository.findWishListWithProductInfo(userId);
            List<WishDTO> wishList = new ArrayList<>();

            for (Object[] result : results) {
                WishDTO wishDTO = WishDTO.builder()
                        .wishId((String) result[0])
                        .userId((String) result[1])
                        .productId((Integer) result[2])
                        .createdDate(result[3] != null ? (LocalDateTime) result[3] : null)
                        .productName((String) result[4])
                        .productPrice(result[5] != null ? ((Number) result[5]).intValue() : null)
                        .salePrice(result[6] != null ? ((Number) result[6]).intValue() : null)
                        .mainImage((String) result[7])
                        .productStatus((String) result[8])
                        .build();
                wishList.add(wishDTO);
            }

            return wishList;
        } catch (Exception e) {
            log.error("찜한 상품 목록 조회 실패 - userId: {}", userId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 사용자의 찜한 상품 개수
     */
    public Long getUserWishCount(String userId) {
        try {
            return wishRepository.countByUserId(userId);
        } catch (Exception e) {
            log.error("찜한 상품 개수 조회 실패 - userId: {}", userId, e);
            return 0L;
        }
    }

    /**
     * 특정 상품의 찜 개수
     */
    public Long getProductWishCount(Integer productId) {
        try {
            return wishRepository.countByProductId(productId);
        } catch (Exception e) {
            log.error("상품 찜 개수 조회 실패 - productId: {}", productId, e);
            return 0L;
        }
    }

    /**
     * 사용자의 모든 찜하기 삭제
     */
    @Transactional
    public boolean clearUserWishlist(String userId) {
        try {
            wishRepository.deleteByUserId(userId);
            return true;
        } catch (Exception e) {
            log.error("찜하기 전체 삭제 실패 - userId: {}", userId, e);
            return false;
        }
    }
}