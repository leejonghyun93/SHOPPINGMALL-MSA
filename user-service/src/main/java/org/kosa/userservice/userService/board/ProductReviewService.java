package org.kosa.userservice.userService.board;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.board.PageRequestDto;
import org.kosa.userservice.dto.board.ProductReviewDto;
import org.kosa.userservice.mapper.ProductReviewMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewMapper productReviewMapper;

    // 기존 메서드들 그대로 유지, import만 수정
    public List<ProductReviewDto> getPagedBoards(PageRequestDto requestDto) {
        log.info("서비스: getPagedBoards 호출됨 - {}", requestDto);

        try {
            if (requestDto != null) {
                requestDto.validate();
                log.info("서비스: 유효성 검사 완료 - {}", requestDto);
            }

            List<ProductReviewDto> result = productReviewMapper.selectPagedBoards(requestDto);
            log.info("서비스: DB 조회 결과 {} 건", result.size());
            return result;

        } catch (Exception e) {
            log.error("서비스: getPagedBoards DB 조회 중 에러 발생", e);
            return new ArrayList<>();
        }
    }
    public List<ProductReviewDto> getPagedBoards(int page, int size, String searchValue, String sortBy) {
        log.info("서비스: getPagedBoards 오버로드 메서드 호출됨");

        PageRequestDto requestDto = PageRequestDto.builder()
                .page(page)
                .size(size)
                .searchValue(searchValue)
                .sortBy(sortBy)
                .build();

        return getPagedBoards(requestDto);
    }

    // 상품별 리뷰 조회 메서드 - startRow 계산 추가
    public List<ProductReviewDto> getProductReviews(String productId, int page, int size, String sortBy) {
        log.info("서비스: getProductReviews 호출됨 - productId: {}, page: {}, size: {}", productId, page, size);

        try {
            // startRow 계산 (page는 1부터 시작)
            int startRow = (page - 1) * size;
            log.info("서비스: 계산된 startRow: {}", startRow);

            // 매퍼 호출 시 startRow 전달
            List<ProductReviewDto> result = productReviewMapper.selectProductReviewsByStringId(
                    productId, startRow, size, sortBy);

            log.info("서비스: 상품 {} DB 조회 결과 {} 건", productId, result.size());

            // 결과가 있으면 첫 번째 데이터 로깅
            if (!result.isEmpty()) {
                ProductReviewDto first = result.get(0);
                log.info("서비스: 첫 번째 결과 - reviewId: {}, productId: {}, title: {}",
                        first.getReviewId(), first.getProductId(), first.getTitle());
            }

            return result;

        } catch (Exception e) {
            log.error("서비스: getProductReviews DB 조회 중 에러 발생 - productId: {}", productId, e);
            return new ArrayList<>();
        }
    }

    public boolean verifyPurchase(String userId, String productId) {
        log.info("서비스: verifyPurchase 호출됨 - userId: {}, productId: {}", userId, productId);

        try {
            int count = productReviewMapper.checkPurchaseVerification(userId, productId);
            boolean isPurchased = count > 0;

            log.info("서비스: 구매 인증 결과 - userId: {}, productId: {}, 구매여부: {}",
                    userId, productId, isPurchased);

            return isPurchased;
        } catch (Exception e) {
            log.error("서비스: 구매 인증 확인 중 에러 발생", e);
            return false;
        }
    }

    // 회원 이름 조회
    public String getMemberNameByUserId(String userId) {
        log.info("서비스: getMemberNameByUserId 호출됨 - userId: {}", userId);

        try {
            String memberName = productReviewMapper.selectMemberNameByUserId(userId);
            log.info("서비스: 회원 이름 조회 결과 - userId: {}, name: {}", userId, memberName);
            return memberName;
        } catch (Exception e) {
            log.error("서비스: 회원 이름 조회 중 에러 발생", e);
            return null;
        }
    }

    //  구매 정보 조회 (선택사항)
    public Map<String, Object> getPurchaseInfo(String userId, String productId) {
        try {
            Map<String, Object> purchaseInfo = productReviewMapper.getPurchaseInfo(userId, productId);
            log.info("서비스: 구매 정보 조회 결과 - {}", purchaseInfo);
            return purchaseInfo;
        } catch (Exception e) {
            log.error("서비스: 구매 정보 조회 중 에러 발생", e);
            return null;
        }
    }
    public String createReview(ProductReviewDto reviewDto) {
        log.info("서비스: createReview 호출됨 - productId: {}, userId: {}, authorName: {}",
                reviewDto.getProductId(), reviewDto.getUserId(), reviewDto.getAuthorName());

        try {
            // 리뷰 ID 생성
            String reviewId = generateReviewId();
            reviewDto.setReviewId(reviewId);

            //  필수 필드 검증 (이미 컨트롤러에서 설정됨)
            if (reviewDto.getUserId() == null || reviewDto.getUserId().trim().isEmpty()) {
                throw new RuntimeException("사용자 ID가 없습니다.");
            }

            if (reviewDto.getAuthorName() == null || reviewDto.getAuthorName().trim().isEmpty()) {
                throw new RuntimeException("작성자 이름이 없습니다.");
            }

            // 기본값 설정
            if (reviewDto.getReviewStatus() == null) {
                reviewDto.setReviewStatus("ACTIVE");
            }
            if (reviewDto.getHelpfulCount() == null) {
                reviewDto.setHelpfulCount(0);
            }
            if (reviewDto.getViewCount() == null) {
                reviewDto.setViewCount(0);
            }
            if (reviewDto.getIsPhoto() == null) {
                reviewDto.setIsPhoto("N");
            }
            if (reviewDto.getIsVerified() == null) {
                reviewDto.setIsVerified("N");
            }

            // 생성일, 수정일 설정
            LocalDateTime now = LocalDateTime.now();
            reviewDto.setCreatedDate(now);
            reviewDto.setUpdatedDate(now);

            log.info("서비스: 리뷰 등록 준비 완료 - reviewId: {}, 작성자: {}",
                    reviewId, reviewDto.getAuthorName());

            // DB 삽입
            int result = productReviewMapper.insertReview(reviewDto);

            if (result > 0) {
                log.info("서비스: 리뷰 등록 성공 - reviewId: {}", reviewId);
                return reviewId;
            } else {
                throw new RuntimeException("리뷰 등록에 실패했습니다.");
            }

        } catch (Exception e) {
            log.error("서비스: createReview 중 에러 발생", e);
            throw new RuntimeException("리뷰 등록 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 리뷰 상세 조회
    public ProductReviewDto getReviewById(String reviewId) {
        log.info("서비스: getReviewById 호출됨 - reviewId: {}", reviewId);

        try {
            ProductReviewDto result = productReviewMapper.selectReviewById(reviewId);
            log.info("서비스: 리뷰 조회 결과 - {}", result != null ? "성공" : "없음");
            return result;

        } catch (Exception e) {
            log.error("서비스: getReviewById 중 에러 발생", e);
            return null;
        }
    }

    //  리뷰 수정
    public boolean updateReview(ProductReviewDto reviewDto) {
        log.info("서비스: updateReview 호출됨 - reviewId: {}", reviewDto.getReviewId());

        try {
            // 수정일 설정
            reviewDto.setUpdatedDate(LocalDateTime.now());

            // DB 업데이트
            int result = productReviewMapper.updateReview(reviewDto);

            boolean success = result > 0;
            log.info("서비스: 리뷰 수정 결과 - {}", success ? "성공" : "실패");
            return success;

        } catch (Exception e) {
            log.error("서비스: updateReview 중 에러 발생", e);
            return false;
        }
    }

    //  리뷰 삭제
    public boolean deleteReview(String reviewId) {
        log.info("서비스: deleteReview 호출됨 - reviewId: {}", reviewId);

        try {
            // DB 삭제
            int result = productReviewMapper.deleteReview(reviewId);

            boolean success = result > 0;
            log.info("서비스: 리뷰 삭제 결과 - {}", success ? "성공" : "실패");
            return success;

        } catch (Exception e) {
            log.error("서비스: deleteReview 중 에러 발생", e);
            return false;
        }
    }

    // 상품별 리뷰 개수 조회
    public int getProductReviewCount(String productId) {
        try {
            int count = productReviewMapper.getProductReviewCountByStringId(productId);
            log.info("서비스: 상품 {} 리뷰 개수: {}", productId, count);
            return count;
        } catch (Exception e) {
            log.error("서비스: 상품별 리뷰 개수 조회 중 에러 발생", e);
            return 0;
        }
    }

    // 전체 리뷰 개수 조회
    public int getTotalCount(String searchValue) {
        try {
            return productReviewMapper.getTotalCount(searchValue);
        } catch (Exception e) {
            log.error("서비스: 전체 리뷰 개수 조회 중 에러 발생", e);
            return 0;
        }
    }

    //  리뷰 ID 생성 메서드
    private String generateReviewId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 1000);
        return "R" + timestamp + String.format("%03d", random);
    }

    //  Boolean 값을 String으로 변환하는 유틸리티 메서드들 (필요한 경우)
    private String booleanToString(boolean value) {
        return value ? "Y" : "N";
    }

    private boolean stringToBoolean(String value) {
        return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }
}

