package org.kosa.commerceservice.controller.product;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.commerceservice.dto.ApiResponse;
import org.kosa.commerceservice.dto.product.WishDTO;
import org.kosa.commerceservice.service.product.WishService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Tag(name = "찜하기 API", description = "상품 찜하기 관리 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
public class WishController {

    private final WishService wishService;

    @Value("${jwt.secret:rrYd2zPDUkx7BUhgDsOTxHCbsBkeTgE/uoARWYSqBjU=}")
    private String jwtSecret;

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private String getUserIdFromToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Authorization 헤더가 없거나 Bearer 형식이 아님");
                return null;
            }

            String token = authHeader.substring(7);
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 사용자 ID 추출 (여러 필드에서 시도)
            String userId = claims.getSubject();
            if (isValidUserId(userId)) {
                log.info("사용자 ID 추출 성공 (subject): {}", userId);
                return userId;
            }

            userId = claims.get("username", String.class);
            if (isValidUserId(userId)) {
                log.info("사용자 ID 추출 성공 (username): {}", userId);
                return userId;
            }

            userId = claims.get("userId", String.class);
            if (isValidUserId(userId)) {
                log.info("사용자 ID 추출 성공 (userId): {}", userId);
                return userId;
            }

            log.error("JWT 토큰에서 유효한 사용자 ID를 찾을 수 없음. Claims: {}", claims);
            return null;

        } catch (Exception e) {
            log.error("JWT 토큰 파싱 실패", e);
            return null;
        }
    }

    private boolean isValidUserId(String userId) {
        return userId != null &&
                !userId.trim().isEmpty() &&
                !"null".equals(userId) &&
                !"undefined".equals(userId);
    }

    @Operation(summary = "찜하기 추가", description = "상품을 찜하기 목록에 추가합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추가 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<String>> addToWishlist(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "찜하기 추가 요청", required = true) @RequestBody Map<String, Object> request) {
        try {
            log.info("찜하기 추가 요청 - AuthHeader 존재: {}", authHeader != null);

            String userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                log.warn("사용자 인증 실패 - AuthHeader: {}", authHeader);
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            Object productIdObj = request.get("productId");
            if (productIdObj == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("상품 ID가 필요합니다."));
            }

            // String → Integer로 변경
            Integer productId;
            try {
                if (productIdObj instanceof Integer) {
                    productId = (Integer) productIdObj;
                } else if (productIdObj instanceof String) {
                    productId = Integer.parseInt((String) productIdObj);
                } else {
                    productId = Integer.parseInt(productIdObj.toString());
                }
            } catch (NumberFormatException e) {
                log.error("잘못된 상품 ID 형식: {}", productIdObj);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("올바른 상품 ID를 입력해주세요."));
            }

            log.info("찜하기 추가 처리 - userId: {}, productId: {}", userId, productId);

            boolean success = wishService.addToWishlist(userId, productId);

            if (success) {
                log.info("찜하기 추가 성공 - userId: {}, productId: {}", userId, productId);
                return ResponseEntity.ok(ApiResponse.success("찜하기가 추가되었습니다."));
            } else {
                log.info("이미 찜한 상품 - userId: {}, productId: {}", userId, productId);
                return ResponseEntity.ok(ApiResponse.error("이미 찜한 상품입니다."));
            }
        } catch (Exception e) {
            log.error("찜하기 추가 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("찜하기 추가에 실패했습니다."));
        }
    }

    @Operation(summary = "찜하기 해제", description = "상품을 찜하기 목록에서 제거합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "찜하지 않은 상품")
    })
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<String>> removeFromWishlist(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "상품 ID", required = true, example = "1") @PathVariable Integer productId) {
        try {
            log.info("찜하기 해제 요청 - productId: {}, AuthHeader 존재: {}", productId, authHeader != null);

            String userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            boolean success = wishService.removeFromWishlist(userId, productId);

            if (success) {
                log.info("찜하기 해제 성공 - userId: {}, productId: {}", userId, productId);
                return ResponseEntity.ok(ApiResponse.success("찜하기가 해제되었습니다."));
            } else {
                log.info("찜하지 않은 상품 - userId: {}, productId: {}", userId, productId);
                return ResponseEntity.ok(ApiResponse.error("찜하지 않은 상품입니다."));
            }
        } catch (Exception e) {
            log.error("찜하기 해제 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("찜하기 해제에 실패했습니다."));
        }
    }

    @Operation(summary = "찜하기 상태 확인", description = "특정 상품의 찜하기 상태를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Boolean.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> checkWishlistStatus(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Parameter(description = "상품 ID", required = true, example = "1") @PathVariable Integer productId) {
        try {
            log.info("찜하기 상태 확인 요청 - productId: {}, AuthHeader 존재: {}", productId, authHeader != null);

            String userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                log.warn("찜하기 상태 확인 - 인증 실패");
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            boolean isWishlisted = wishService.isWishlisted(userId, productId);
            log.info("찜하기 상태 확인 결과 - userId: {}, productId: {}, isWishlisted: {}", userId, productId, isWishlisted);

            return ResponseEntity.ok(ApiResponse.success(isWishlisted));
        } catch (Exception e) {
            log.error("찜하기 상태 확인 실패", e);
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    @Operation(summary = "찜한 상품 목록 조회", description = "사용자의 찜한 상품 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = WishDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<WishDTO>>> getUserWishlist(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("찜한 상품 목록 조회 요청 - AuthHeader 존재: {}", authHeader != null);

            String userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            List<WishDTO> wishlist = wishService.getUserWishlist(userId);
            log.info("찜한 상품 목록 조회 성공 - userId: {}, 개수: {}", userId, wishlist.size());

            return ResponseEntity.ok(ApiResponse.success(wishlist));
        } catch (Exception e) {
            log.error("찜한 상품 목록 조회 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("찜한 상품 목록 조회에 실패했습니다."));
        }
    }

    @Operation(summary = "찜한 상품 개수 조회", description = "사용자의 찜한 상품 개수를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Long.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> getUserWishCount(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("찜한 상품 개수 조회 요청 - AuthHeader 존재: {}", authHeader != null);

            String userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            Long count = wishService.getUserWishCount(userId);
            log.info("찜한 상품 개수 조회 성공 - userId: {}, 개수: {}", userId, count);

            return ResponseEntity.ok(ApiResponse.success(count));
        } catch (Exception e) {
            log.error("찜한 상품 개수 조회 실패", e);
            return ResponseEntity.ok(ApiResponse.success(0L));
        }
    }

    @Operation(summary = "찜하기 전체 삭제", description = "사용자의 모든 찜하기 항목을 삭제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearWishlist(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("찜하기 전체 삭제 요청 - AuthHeader 존재: {}", authHeader != null);

            String userId = getUserIdFromToken(authHeader);
            if (userId == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("로그인이 필요합니다."));
            }

            boolean success = wishService.clearUserWishlist(userId);

            if (success) {
                log.info("찜하기 전체 삭제 성공 - userId: {}", userId);
                return ResponseEntity.ok(ApiResponse.success("찜한 상품이 모두 삭제되었습니다."));
            } else {
                return ResponseEntity.internalServerError()
                        .body(ApiResponse.error("찜한 상품 삭제에 실패했습니다."));
            }
        } catch (Exception e) {
            log.error("찜한 상품 전체 삭제 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("찜한 상품 삭제에 실패했습니다."));
        }
    }
}