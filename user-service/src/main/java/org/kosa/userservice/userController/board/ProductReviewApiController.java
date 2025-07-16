package org.kosa.userservice.userController.board;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.board.ProductReviewDto;
import org.kosa.userservice.userService.board.ProductReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Tag(name = "상품 리뷰", description = "상품 리뷰 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/board")
@RequiredArgsConstructor
public class ProductReviewApiController {

    private final ProductReviewService productReviewService;

    @Value("${jwt.secret:rrYd2zPDUkx7BUhgDsOTxHCbsBkeTgE/uoARWYSqBjU=}")
    private String jwtSecret;

    private String extractUserIdFromJWT(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }

            String token = authHeader.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    @Operation(summary = "서비스 상태 확인", description = "Board Service 상태를 확인합니다")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Board Service is running");
    }

    @Operation(summary = "리뷰 목록 조회", description = "페이징과 검색 조건으로 리뷰 목록을 조회합니다")
    @GetMapping("/list")
    public ResponseEntity<List<ProductReviewDto>> list(
            @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "검색어") @RequestParam(required = false) String searchValue,
            @Parameter(description = "정렬 기준") @RequestParam(required = false) String sortBy
    ) {
        try {
            List<ProductReviewDto> reviews = productReviewService.getPagedBoards(page, size, searchValue, sortBy);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "상품별 리뷰 조회", description = "특정 상품의 리뷰 목록을 조회합니다")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductReviewDto>> getProductReviews(
            @Parameter(description = "상품 ID", required = true) @PathVariable Integer productId,
            @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        try {
            List<ProductReviewDto> reviews = productReviewService.getProductReviews(productId, page, size, sortBy);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "리뷰 상세 조회", description = "리뷰 ID로 상세 정보를 조회합니다")
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ProductReviewDto> getReview(
            @Parameter(description = "리뷰 ID", required = true) @PathVariable String reviewId) {
        try {
            ProductReviewDto review = productReviewService.getReviewById(reviewId);

            if (review == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            return ResponseEntity.ok(review);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "리뷰 작성", description = "새로운 상품 리뷰를 작성합니다")
    @SecurityRequirement(name = "JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "리뷰 등록 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "구매 인증 실패"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/reviews")
    public ResponseEntity<Map<String, Object>> createReview(
            @RequestBody ProductReviewDto reviewDto,
            HttpServletRequest request) {

        String userId = extractUserIdFromJWT(request);
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다. 로그인 후 다시 시도해주세요.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            if (reviewDto.getProductId() == null) {
                response.put("success", false);
                response.put("message", "상품 ID는 필수입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            boolean isPurchased = productReviewService.verifyPurchase(userId, reviewDto.getProductId());
            if (!isPurchased) {
                response.put("success", false);
                response.put("message", "해당 상품을 구매하고 배송완료된 고객만 리뷰를 작성할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

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

            String memberName = productReviewService.getMemberNameByUserId(userId);
            if (memberName == null || memberName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "유효하지 않은 사용자입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            reviewDto.setUserId(userId);
            reviewDto.setAuthorName(memberName);

            String reviewId = productReviewService.createReview(reviewDto);

            response.put("success", true);
            response.put("message", "리뷰가 성공적으로 등록되었습니다.");
            response.put("reviewId", reviewId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "리뷰 등록 중 오류가 발생했습니다: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "리뷰 수정", description = "기존 리뷰를 수정합니다")
    @SecurityRequirement(name = "JWT")
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<Map<String, Object>> updateReview(
            @Parameter(description = "리뷰 ID", required = true) @PathVariable String reviewId,
            @RequestBody ProductReviewDto reviewDto,
            HttpServletRequest request) {

        String userId = extractUserIdFromJWT(request);
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            ProductReviewDto existingReview = productReviewService.getReviewById(reviewId);
            if (existingReview == null) {
                response.put("success", false);
                response.put("message", "해당 리뷰를 찾을 수 없습니다.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            if (!userId.equals(existingReview.getUserId())) {
                response.put("success", false);
                response.put("message", "본인의 리뷰만 수정할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            boolean isPurchased = productReviewService.verifyPurchase(userId, existingReview.getProductId());
            if (!isPurchased) {
                response.put("success", false);
                response.put("message", "구매 인증이 확인되지 않습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            reviewDto.setReviewId(reviewId);

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
            } else {
                response.put("success", false);
                response.put("message", "리뷰 수정에 실패했습니다.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "리뷰 수정 중 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "리뷰 삭제", description = "리뷰를 삭제합니다")
    @SecurityRequirement(name = "JWT")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Map<String, Object>> deleteReview(
            @Parameter(description = "리뷰 ID", required = true) @PathVariable String reviewId,
            HttpServletRequest request) {

        String userId = extractUserIdFromJWT(request);
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            ProductReviewDto existingReview = productReviewService.getReviewById(reviewId);
            if (existingReview == null) {
                response.put("success", false);
                response.put("message", "해당 리뷰를 찾을 수 없습니다.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            if (!userId.equals(existingReview.getUserId())) {
                response.put("success", false);
                response.put("message", "본인의 리뷰만 삭제할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

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
            } else {
                response.put("success", false);
                response.put("message", "리뷰 삭제에 실패했습니다.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "리뷰 삭제 중 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "전체 리뷰 조회 (디버그용)", description = "디버그용 전체 리뷰 목록 조회")
    @GetMapping("/debug/all")
    public ResponseEntity<List<ProductReviewDto>> getAllReviewsDebug() {
        try {
            List<ProductReviewDto> allReviews = productReviewService.getPagedBoards(1, 100, null, "createdAt");
            return ResponseEntity.ok(allReviews);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "테스트 엔드포인트", description = "서비스 테스트용 엔드포인트")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Board Service Test OK - " + System.currentTimeMillis());
    }
}