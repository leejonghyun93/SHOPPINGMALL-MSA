package org.kosa.userservice.userController.board;

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

@Slf4j
@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class ProductQnaController {

    private final ProductQnaService productQnaService;

    @Value("${jwt.secret:verySecretKeyThatIsAtLeast32BytesLong1234}")
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

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("QnA Service is running");
    }

    @GetMapping("/list")
    public ResponseEntity<List<ProductQnaDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String sortBy
    ) {
        try {
            List<ProductQnaDto> qnas = productQnaService.getPagedQnas(page, size, searchValue, sortBy);
            return ResponseEntity.ok(qnas);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductQnaDto>> getProductQnas(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        try {
            List<ProductQnaDto> qnas = productQnaService.getProductQnas(productId, page, size, sortBy);
            return ResponseEntity.ok(qnas);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{qnaId}")
    public ResponseEntity<ProductQnaDto> getQna(@PathVariable String qnaId) {
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

    @PutMapping("/{qnaId}")
    public ResponseEntity<Map<String, Object>> updateQna(
            @PathVariable String qnaId,
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

    @DeleteMapping("/{qnaId}")
    public ResponseEntity<Map<String, Object>> deleteQna(
            @PathVariable String qnaId,
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

    @GetMapping("/my")
    public ResponseEntity<List<ProductQnaDto>> getMyQnas(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
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

    @GetMapping("/debug/all")
    public ResponseEntity<List<ProductQnaDto>> getAllQnasDebug() {
        try {
            List<ProductQnaDto> allQnas = productQnaService.getPagedQnas(1, 100, null, "createdAt");
            return ResponseEntity.ok(allQnas);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

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