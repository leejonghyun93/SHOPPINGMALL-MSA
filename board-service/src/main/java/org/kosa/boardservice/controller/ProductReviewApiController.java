package org.kosa.boardservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.boardservice.dto.ProductReviewDto;
import org.kosa.boardservice.service.ProductReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class ProductReviewApiController {

    private final ProductReviewService productReviewService;

    // 헬스 체크 엔드포인트
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("🏥 Board Service Health Check 호출됨");
        return ResponseEntity.ok("Board Service is running");
    }

    // 🔥 게시글 리스트 조회 - 강화된 로깅 및 디버깅
    @GetMapping("/list")
    public ResponseEntity<List<ProductReviewDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String sortBy
    ) {
        log.info("🚀 === Board API 호출 시작 ===");
        log.info("📋 요청 파라미터 - page: {}, size: {}, searchValue: {}, sortBy: {}",
                page, size, searchValue, sortBy);

        try {
            List<ProductReviewDto> reviews = productReviewService.getPagedBoards(page, size, searchValue, sortBy);
            log.info("✅ 조회된 리뷰 개수: {}", reviews.size());

            // 첫 번째 리뷰 데이터 로깅 (있다면)
            if (!reviews.isEmpty()) {
                ProductReviewDto firstReview = reviews.get(0);
                log.info("📄 첫 번째 리뷰 샘플: ID={}, ProductID={}, Title={}",
                        firstReview.getReviewId(), firstReview.getProductId(), firstReview.getTitle());
            }

            log.info("🏁 === Board API 호출 완료 ===");
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            log.error("❌ Board API 호출 중 에러 발생: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 상품별 리뷰 조회 - 강화된 로깅
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductReviewDto>> getProductReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        log.info("🚀 === 상품별 리뷰 API 호출 시작 ===");
        log.info("🛍️ 상품 ID: {}, page: {}, size: {}, sortBy: {}", productId, page, size, sortBy);

        try {
            List<ProductReviewDto> reviews = productReviewService.getProductReviews(productId, page, size, sortBy);
            log.info("✅ 상품 {}의 리뷰 개수: {}", productId, reviews.size());

            // 첫 번째 리뷰 데이터 로깅 (있다면)
            if (!reviews.isEmpty()) {
                ProductReviewDto firstReview = reviews.get(0);
                log.info("📄 첫 번째 리뷰 샘플: ID={}, Title={}",
                        firstReview.getReviewId(), firstReview.getTitle());
            }

            log.info("🏁 === 상품별 리뷰 API 호출 완료 ===");
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            log.error("❌ 상품별 리뷰 API 호출 중 에러 발생: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 리뷰 상세 조회
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ProductReviewDto> getReview(@PathVariable String reviewId) {
        log.info("🔍 === 리뷰 상세 조회 API 호출 ===");
        log.info("📋 리뷰 ID: {}", reviewId);

        try {
            ProductReviewDto review = productReviewService.getReviewById(reviewId);

            if (review == null) {
                log.warn("⚠️ 리뷰를 찾을 수 없음: ID={}", reviewId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            log.info("✅ 리뷰 조회 성공: ID={}, Title={}", review.getReviewId(), review.getTitle());
            return ResponseEntity.ok(review);

        } catch (Exception e) {
            log.error("❌ 리뷰 조회 중 에러 발생: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 4. 구매 인증이 적용된 컨트롤러
    @PostMapping("/reviews")
    public ResponseEntity<Map<String, Object>> createReview(
            @RequestBody ProductReviewDto reviewDto,
            HttpServletRequest request) {

        log.info("📝 === 리뷰 등록 API 호출 시작 ===");
        log.info("📋 등록할 리뷰: productId={}, title={}, rating={}",
                reviewDto.getProductId(), reviewDto.getTitle(), reviewDto.getRating());

        // JWT 헤더에서 사용자 ID 추출
        String userId = request.getHeader("X-User-Id");

        log.info("🔑 JWT 사용자 ID: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            // 🔥 JWT 인증 확인
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다. 로그인 후 다시 시도해주세요.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // 필수 필드 검증
            if (reviewDto.getProductId() == null || reviewDto.getProductId().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "상품 ID는 필수입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // 🔥 구매 인증 확인 - 핵심!
            boolean isPurchased = productReviewService.verifyPurchase(userId, reviewDto.getProductId());
            if (!isPurchased) {
                response.put("success", false);
                response.put("message", "해당 상품을 구매하고 배송완료된 고객만 리뷰를 작성할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            log.info("✅ 구매 인증 통과 - userId: {}, productId: {}", userId, reviewDto.getProductId());

            // 제목, 내용, 평점 검증
            if (reviewDto.getTitle() == null || reviewDto.getTitle().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "리뷰 제목은 필수입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (reviewDto.getRating() == null || reviewDto.getRating() < 1 || reviewDto.getRating() > 5) {
                response.put("success", false);
                response.put("message", "평점은 1~5 사이의 값이어야 합니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // 🔥 실제 회원 테이블에서 사용자 이름 조회
            String memberName = productReviewService.getMemberNameByUserId(userId);
            if (memberName == null || memberName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "유효하지 않은 사용자입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // 사용자 정보 설정
            reviewDto.setUserId(userId);
            reviewDto.setAuthorName(memberName);

            log.info("✅ 회원 정보 설정 완료 - userId: {}, memberName: {}", userId, memberName);

            String reviewId = productReviewService.createReview(reviewDto);

            response.put("success", true);
            response.put("message", "리뷰가 성공적으로 등록되었습니다.");
            response.put("reviewId", reviewId);

            log.info("✅ 리뷰 등록 성공: ID={}, 작성자: {}", reviewId, memberName);
            log.info("🏁 === 리뷰 등록 API 호출 완료 ===");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 리뷰 등록 중 에러 발생: ", e);
            response.put("success", false);
            response.put("message", "리뷰 등록 중 오류가 발생했습니다: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 5. 리뷰 수정도 동일한 구매 인증 적용
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<Map<String, Object>> updateReview(
            @PathVariable String reviewId,
            @RequestBody ProductReviewDto reviewDto,
            HttpServletRequest request) {

        log.info("✏️ === 리뷰 수정 API 호출 시작 ===");

        String userId = request.getHeader("X-User-Id");
        Map<String, Object> response = new HashMap<>();

        try {
            // 인증 확인
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // 🔥 기존 리뷰 조회하여 상품 ID 확인
            ProductReviewDto existingReview = productReviewService.getReviewById(reviewId);
            if (existingReview == null) {
                response.put("success", false);
                response.put("message", "해당 리뷰를 찾을 수 없습니다.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // 🔥 본인 리뷰인지 확인
            if (!userId.equals(existingReview.getUserId())) {
                response.put("success", false);
                response.put("message", "본인의 리뷰만 수정할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            // 🔥 구매 인증 재확인 (혹시 모를 상황 대비)
            boolean isPurchased = productReviewService.verifyPurchase(userId, existingReview.getProductId());
            if (!isPurchased) {
                response.put("success", false);
                response.put("message", "구매 인증이 확인되지 않습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            // 나머지 수정 로직 진행...
            reviewDto.setReviewId(reviewId);

            // 필수 필드 검증 및 업데이트
            if (reviewDto.getTitle() == null || reviewDto.getTitle().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "리뷰 제목은 필수입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (reviewDto.getRating() == null || reviewDto.getRating() < 1 || reviewDto.getRating() > 5) {
                response.put("success", false);
                response.put("message", "평점은 1~5 사이의 값이어야 합니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            boolean success = productReviewService.updateReview(reviewDto);

            if (success) {
                response.put("success", true);
                response.put("message", "리뷰가 성공적으로 수정되었습니다.");
                log.info("✅ 리뷰 수정 성공: ID={}", reviewId);
            } else {
                response.put("success", false);
                response.put("message", "리뷰 수정에 실패했습니다.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 리뷰 수정 중 에러 발생: ", e);
            response.put("success", false);
            response.put("message", "리뷰 수정 중 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 6. 리뷰 삭제도 동일한 구매 인증 적용
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Map<String, Object>> deleteReview(
            @PathVariable String reviewId,
            HttpServletRequest request) {

        log.info("🗑️ === 리뷰 삭제 API 호출 시작 ===");

        String userId = request.getHeader("X-User-Id");
        Map<String, Object> response = new HashMap<>();

        try {
            // 인증 확인
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // 🔥 기존 리뷰 조회
            ProductReviewDto existingReview = productReviewService.getReviewById(reviewId);
            if (existingReview == null) {
                response.put("success", false);
                response.put("message", "해당 리뷰를 찾을 수 없습니다.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // 🔥 본인 리뷰인지 확인
            if (!userId.equals(existingReview.getUserId())) {
                response.put("success", false);
                response.put("message", "본인의 리뷰만 삭제할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            // 🔥 구매 인증 재확인
            boolean isPurchased = productReviewService.verifyPurchase(userId, existingReview.getProductId());
            if (!isPurchased) {
                response.put("success", false);
                response.put("message", "구매 인증이 확인되지 않습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            boolean success = productReviewService.deleteReview(reviewId);

            if (success) {
                response.put("success", true);
                response.put("message", "리뷰가 성공적으로 삭제되었습니다.");
                log.info("✅ 리뷰 삭제 성공: ID={}", reviewId);
            } else {
                response.put("success", false);
                response.put("message", "리뷰 삭제에 실패했습니다.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ 리뷰 삭제 중 에러 발생: ", e);
            response.put("success", false);
            response.put("message", "리뷰 삭제 중 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    // 🔥 디버깅용 전체 리뷰 조회 (페이징 없음)
    @GetMapping("/debug/all")
    public ResponseEntity<List<ProductReviewDto>> getAllReviewsDebug() {
        log.info("🔍 === 디버깅용 전체 리뷰 조회 ===");

        try {
            List<ProductReviewDto> allReviews = productReviewService.getPagedBoards(1, 100, null, "createdAt");
            log.info("📊 전체 리뷰 개수: {}", allReviews.size());

            // 각 상품별 리뷰 개수 로깅
            allReviews.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            ProductReviewDto::getProductId,
                            java.util.stream.Collectors.counting()
                    ))
                    .forEach((productId, count) ->
                            log.info("🛍️ 상품 {}: {}개 리뷰", productId, count)
                    );

            return ResponseEntity.ok(allReviews);
        } catch (Exception e) {
            log.error("❌ 디버깅 조회 실패: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 디버깅용 테스트 엔드포인트
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("🧪 Board Service 테스트 엔드포인트 호출됨");
        return ResponseEntity.ok("Board Service Test OK - " + System.currentTimeMillis());
    }
}