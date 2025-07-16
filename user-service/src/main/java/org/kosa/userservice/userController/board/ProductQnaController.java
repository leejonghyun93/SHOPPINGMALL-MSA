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
import org.kosa.userservice.dto.board.ProductQnaDto;
import org.kosa.userservice.userService.board.ProductQnaService;
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

@Tag(name = "상품 Q&A", description = "상품 문의 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class ProductQnaController {

    private final ProductQnaService productQnaService;

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

    @Operation(summary = "서비스 상태 확인", description = "QnA Service 상태를 확인합니다")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("QnA Service is running");
    }

    @Operation(summary = "Q&A 목록 조회", description = "페이징과 검색 조건으로 Q&A 목록을 조회합니다")
    @GetMapping("/list")
    public ResponseEntity<List<ProductQnaDto>> list(
            @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "검색어") @RequestParam(required = false) String searchValue,
            @Parameter(description = "정렬 기준") @RequestParam(required = false) String sortBy
    ) {
        try {
            List<ProductQnaDto> qnas = productQnaService.getPagedQnas(page, size, searchValue, sortBy);
            return ResponseEntity.ok(qnas);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "상품별 Q&A 조회", description = "특정 상품의 Q&A 목록을 조회합니다")
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductQnaDto>> getProductQnas(
            @Parameter(description = "상품 ID", required = true) @PathVariable Integer productId,
            @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준", example = "createdAt") @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        try {
            List<ProductQnaDto> qnas = productQnaService.getProductQnas(productId, page, size, sortBy);
            return ResponseEntity.ok(qnas);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Q&A 상세 조회", description = "Q&A ID로 상세 정보를 조회합니다")
    @GetMapping("/{qnaId}")
    public ResponseEntity<ProductQnaDto> getQna(
            @Parameter(description = "Q&A ID", required = true) @PathVariable String qnaId) {
        try {
            ProductQnaDto qna = productQnaService.getQnaById(qnaId, true);

            if (qna == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            return ResponseEntity.ok(qna);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Q&A 작성", description = "새로운 상품 문의를 작성합니다")
    @SecurityRequirement(name = "JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "문의 등록 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "구매 인증 실패"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> createQna(
            @RequestBody ProductQnaDto qnaDto,
            HttpServletRequest request) {

        String userId = extractUserIdFromJWT(request);
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다. 로그인 후 다시 시도해주세요.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            if (qnaDto.getProductId() == null) {
                response.put("success", false);
                response.put("message", "상품 ID는 필수입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            boolean isPurchased = productQnaService.verifyPurchase(userId, qnaDto.getProductId());
            if (!isPurchased) {
                response.put("success", false);
                response.put("message", "해당 상품을 구매하고 배송완료된 고객만 문의를 작성할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            if (qnaDto.getTitle() == null || qnaDto.getTitle().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "문의 제목은 필수입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (qnaDto.getContent() == null || qnaDto.getContent().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "문의 내용은 필수입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            String memberName = productQnaService.getMemberNameByUserId(userId);
            if (memberName == null || memberName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "유효하지 않은 사용자입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            qnaDto.setUserId(userId);
            qnaDto.setAuthorName(memberName);
            qnaDto.setAuthorIp(getClientIpAddress(request));

            String qnaId = productQnaService.createQna(qnaDto);

            response.put("success", true);
            response.put("message", "문의가 성공적으로 등록되었습니다.");
            response.put("qnaId", qnaId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "문의 등록 중 오류가 발생했습니다: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Q&A 수정", description = "기존 문의를 수정합니다")
    @SecurityRequirement(name = "JWT")
    @PutMapping("/{qnaId}")
    public ResponseEntity<Map<String, Object>> updateQna(
            @Parameter(description = "Q&A ID", required = true) @PathVariable String qnaId,
            @RequestBody ProductQnaDto qnaDto,
            HttpServletRequest request) {

        String userId = extractUserIdFromJWT(request);
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            ProductQnaDto existingQna = productQnaService.getQnaById(qnaId, false);
            if (existingQna == null) {
                response.put("success", false);
                response.put("message", "해당 문의를 찾을 수 없습니다.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            if (!userId.equals(existingQna.getUserId())) {
                response.put("success", false);
                response.put("message", "본인의 문의만 수정할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            boolean isPurchased = productQnaService.verifyPurchase(userId, existingQna.getProductId());
            if (!isPurchased) {
                response.put("success", false);
                response.put("message", "구매 인증이 확인되지 않습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            qnaDto.setQnaId(qnaId);

            if (qnaDto.getTitle() == null || qnaDto.getTitle().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "문의 제목은 필수입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            if (qnaDto.getContent() == null || qnaDto.getContent().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "문의 내용은 필수입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            boolean success = productQnaService.updateQna(qnaDto);

            if (success) {
                response.put("success", true);
                response.put("message", "문의가 성공적으로 수정되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "문의 수정에 실패했습니다.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "문의 수정 중 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Q&A 삭제", description = "문의를 삭제합니다")
    @SecurityRequirement(name = "JWT")
    @DeleteMapping("/{qnaId}")
    public ResponseEntity<Map<String, Object>> deleteQna(
            @Parameter(description = "Q&A ID", required = true) @PathVariable String qnaId,
            HttpServletRequest request) {

        String userId = extractUserIdFromJWT(request);
        Map<String, Object> response = new HashMap<>();

        try {
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            ProductQnaDto existingQna = productQnaService.getQnaById(qnaId, false);
            if (existingQna == null) {
                response.put("success", false);
                response.put("message", "해당 문의를 찾을 수 없습니다.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            if (!userId.equals(existingQna.getUserId())) {
                response.put("success", false);
                response.put("message", "본인의 문의만 삭제할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            boolean isPurchased = productQnaService.verifyPurchase(userId, existingQna.getProductId());
            if (!isPurchased) {
                response.put("success", false);
                response.put("message", "구매 인증이 확인되지 않습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            boolean success = productQnaService.deleteQna(qnaId);

            if (success) {
                response.put("success", true);
                response.put("message", "문의가 성공적으로 삭제되었습니다.");
            } else {
                response.put("success", false);
                response.put("message", "문의 삭제에 실패했습니다.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "문의 삭제 중 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "내 Q&A 조회", description = "로그인한 사용자의 Q&A 목록을 조회합니다")
    @SecurityRequirement(name = "JWT")
    @GetMapping("/my")
    public ResponseEntity<List<ProductQnaDto>> getMyQnas(
            @Parameter(description = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        String userId = extractUserIdFromJWT(request);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            List<ProductQnaDto> myQnas = productQnaService.getUserQnas(userId, page, size);
            return ResponseEntity.ok(myQnas);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "전체 Q&A 조회 (디버그용)", description = "디버그용 전체 Q&A 목록 조회")
    @GetMapping("/debug/all")
    public ResponseEntity<List<ProductQnaDto>> getAllQnasDebug() {
        try {
            List<ProductQnaDto> allQnas = productQnaService.getPagedQnas(1, 100, null, "createdAt");
            return ResponseEntity.ok(allQnas);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "테스트 엔드포인트", description = "서비스 테스트용 엔드포인트")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("QnA Service Test OK - " + System.currentTimeMillis());
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}