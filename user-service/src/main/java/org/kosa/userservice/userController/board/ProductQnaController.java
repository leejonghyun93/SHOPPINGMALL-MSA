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

    // 🔥 JWT 토큰에서 사용자 ID 추출 메서드
    private String extractUserIdFromJWT(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("❌ Authorization 헤더가 없거나 Bearer 형식이 아님");
                return null;
            }

            String token = authHeader.substring(7); // "Bearer " 제거
            log.info("🔍 JWT 토큰 파싱 시도: {}", token.substring(0, Math.min(20, token.length())) + "...");

            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject(); // JWT의 sub 클레임에서 사용자 ID 추출
            log.info("✅ JWT에서 추출된 사용자 ID: {}", userId);

            return userId;
        } catch (Exception e) {
            log.error("❌ JWT 토큰 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    // 헬스 체크 엔드포인트
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        log.info("🏥 QnA Service Health Check 호출됨");
        return ResponseEntity.ok("QnA Service is running");
    }

    // 🔥 Q&A 리스트 조회
    @GetMapping("/list")
    public ResponseEntity<List<ProductQnaDto>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchValue,
            @RequestParam(required = false) String sortBy
    ) {
        log.info("🚀 === Q&A API 호출 시작 ===");
        log.info("📋 요청 파라미터 - page: {}, size: {}, searchValue: {}, sortBy: {}",
                page, size, searchValue, sortBy);

        try {
            List<ProductQnaDto> qnas = productQnaService.getPagedQnas(page, size, searchValue, sortBy);
            log.info("✅ 조회된 Q&A 개수: {}", qnas.size());

            if (!qnas.isEmpty()) {
                ProductQnaDto firstQna = qnas.get(0);
                log.info("📄 첫 번째 Q&A 샘플: ID={}, ProductID={}, Title={}",
                        firstQna.getQnaId(), firstQna.getProductId(), firstQna.getTitle());
            }

            log.info("🏁 === Q&A API 호출 완료 ===");
            return ResponseEntity.ok(qnas);
        } catch (Exception e) {
            log.error("❌ Q&A API 호출 중 에러 발생: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 상품별 Q&A 조회
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductQnaDto>> getProductQnas(
            @PathVariable String productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        log.info("🚀 === 상품별 Q&A API 호출 시작 ===");
        log.info("🛍️ 상품 ID: {}, page: {}, size: {}, sortBy: {}", productId, page, size, sortBy);

        try {
            List<ProductQnaDto> qnas = productQnaService.getProductQnas(productId, page, size, sortBy);
            log.info("✅ 상품 {}의 Q&A 개수: {}", productId, qnas.size());

            if (!qnas.isEmpty()) {
                ProductQnaDto firstQna = qnas.get(0);
                log.info("📄 첫 번째 Q&A 샘플: ID={}, Title={}",
                        firstQna.getQnaId(), firstQna.getTitle());
            }

            log.info("🏁 === 상품별 Q&A API 호출 완료 ===");
            return ResponseEntity.ok(qnas);
        } catch (Exception e) {
            log.error("❌ 상품별 Q&A API 호출 중 에러 발생: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 Q&A 상세 조회
    @GetMapping("/{qnaId}")
    public ResponseEntity<ProductQnaDto> getQna(@PathVariable String qnaId) {
        log.info("🔍 === Q&A 상세 조회 API 호출 ===");
        log.info("📋 Q&A ID: {}", qnaId);

        try {
            ProductQnaDto qna = productQnaService.getQnaById(qnaId, true); // 조회수 증가

            if (qna == null) {
                log.warn("⚠️ Q&A를 찾을 수 없음: ID={}", qnaId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            log.info("✅ Q&A 조회 성공: ID={}, Title={}", qna.getQnaId(), qna.getTitle());
            return ResponseEntity.ok(qna);

        } catch (Exception e) {
            log.error("❌ Q&A 조회 중 에러 발생: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 Q&A 등록 (표준 JWT 방식으로 수정)
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> createQna(
            @RequestBody ProductQnaDto qnaDto,
            HttpServletRequest request) {

        log.info("📝 === Q&A 등록 API 호출 시작 ===");
        log.info("📋 등록할 Q&A: productId={}, title={}, qnaType={}",
                qnaDto.getProductId(), qnaDto.getTitle(), qnaDto.getQnaType());

        // 🔥 표준 JWT에서 사용자 ID 추출
        String userId = extractUserIdFromJWT(request);
        log.info("🔑 JWT에서 추출된 사용자 ID: {}", userId);

        Map<String, Object> response = new HashMap<>();

        try {
            // 🔥 JWT 인증 확인
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다. 로그인 후 다시 시도해주세요.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // 필수 필드 검증
            if (qnaDto.getProductId() == null || qnaDto.getProductId().trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "상품 ID는 필수입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // 🔥 구매 인증 확인 - Q&A도 구매한 상품에만 작성 가능
            boolean isPurchased = productQnaService.verifyPurchase(userId, qnaDto.getProductId());
            if (!isPurchased) {
                response.put("success", false);
                response.put("message", "해당 상품을 구매하고 배송완료된 고객만 문의를 작성할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            log.info("✅ 구매 인증 통과 - userId: {}, productId: {}", userId, qnaDto.getProductId());

            // 제목, 내용 검증
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

            // 🔥 실제 회원 테이블에서 사용자 이름 조회
            String memberName = productQnaService.getMemberNameByUserId(userId);
            if (memberName == null || memberName.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "유효하지 않은 사용자입니다.");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

            // 사용자 정보 설정
            qnaDto.setUserId(userId);
            qnaDto.setAuthorName(memberName);

            // IP 주소 설정 (선택사항)
            String clientIp = getClientIpAddress(request);
            qnaDto.setAuthorIp(clientIp);

            log.info("✅ 회원 정보 설정 완료 - userId: {}, memberName: {}", userId, memberName);

            String qnaId = productQnaService.createQna(qnaDto);

            response.put("success", true);
            response.put("message", "문의가 성공적으로 등록되었습니다.");
            response.put("qnaId", qnaId);

            log.info("✅ Q&A 등록 성공: ID={}, 작성자: {}", qnaId, memberName);
            log.info("🏁 === Q&A 등록 API 호출 완료 ===");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Q&A 등록 중 에러 발생: ", e);
            response.put("success", false);
            response.put("message", "문의 등록 중 오류가 발생했습니다: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 Q&A 수정 (표준 JWT 방식으로 수정)
    @PutMapping("/{qnaId}")
    public ResponseEntity<Map<String, Object>> updateQna(
            @PathVariable String qnaId,
            @RequestBody ProductQnaDto qnaDto,
            HttpServletRequest request) {

        log.info("✏️ === Q&A 수정 API 호출 시작 ===");

        String userId = extractUserIdFromJWT(request);
        Map<String, Object> response = new HashMap<>();

        try {
            // 인증 확인
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // 🔥 기존 Q&A 조회
            ProductQnaDto existingQna = productQnaService.getQnaById(qnaId, false);
            if (existingQna == null) {
                response.put("success", false);
                response.put("message", "해당 문의를 찾을 수 없습니다.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // 🔥 본인 Q&A인지 확인
            if (!userId.equals(existingQna.getUserId())) {
                response.put("success", false);
                response.put("message", "본인의 문의만 수정할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            // 🔥 구매 인증 재확인
            boolean isPurchased = productQnaService.verifyPurchase(userId, existingQna.getProductId());
            if (!isPurchased) {
                response.put("success", false);
                response.put("message", "구매 인증이 확인되지 않습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            // 수정 로직 진행
            qnaDto.setQnaId(qnaId);

            // 필수 필드 검증
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
                log.info("✅ Q&A 수정 성공: ID={}", qnaId);
            } else {
                response.put("success", false);
                response.put("message", "문의 수정에 실패했습니다.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Q&A 수정 중 에러 발생: ", e);
            response.put("success", false);
            response.put("message", "문의 수정 중 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 Q&A 삭제 (표준 JWT 방식으로 수정)
    @DeleteMapping("/{qnaId}")
    public ResponseEntity<Map<String, Object>> deleteQna(
            @PathVariable String qnaId,
            HttpServletRequest request) {

        log.info("🗑️ === Q&A 삭제 API 호출 시작 ===");

        String userId = extractUserIdFromJWT(request);
        Map<String, Object> response = new HashMap<>();

        try {
            // 인증 확인
            if (userId == null || userId.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "인증이 필요합니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            // 🔥 기존 Q&A 조회
            ProductQnaDto existingQna = productQnaService.getQnaById(qnaId, false);
            if (existingQna == null) {
                response.put("success", false);
                response.put("message", "해당 문의를 찾을 수 없습니다.");
                return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
            }

            // 🔥 본인 Q&A인지 확인
            if (!userId.equals(existingQna.getUserId())) {
                response.put("success", false);
                response.put("message", "본인의 문의만 삭제할 수 있습니다.");
                return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
            }

            // 🔥 구매 인증 재확인
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
                log.info("✅ Q&A 삭제 성공: ID={}", qnaId);
            } else {
                response.put("success", false);
                response.put("message", "문의 삭제에 실패했습니다.");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Q&A 삭제 중 에러 발생: ", e);
            response.put("success", false);
            response.put("message", "문의 삭제 중 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 사용자별 Q&A 조회 (표준 JWT 방식으로 수정)
    @GetMapping("/my")
    public ResponseEntity<List<ProductQnaDto>> getMyQnas(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        log.info("🚀 === 내 Q&A 조회 API 호출 시작 ===");

        String userId = extractUserIdFromJWT(request);

        try {
            if (userId == null || userId.trim().isEmpty()) {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }

            List<ProductQnaDto> myQnas = productQnaService.getUserQnas(userId, page, size);
            log.info("✅ 사용자 {} Q&A 조회 결과: {} 건", userId, myQnas.size());

            return ResponseEntity.ok(myQnas);

        } catch (Exception e) {
            log.error("❌ 내 Q&A 조회 중 에러 발생: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 🔥 디버깅용 전체 Q&A 조회
    @GetMapping("/debug/all")
    public ResponseEntity<List<ProductQnaDto>> getAllQnasDebug() {
        log.info("🔍 === 디버깅용 전체 Q&A 조회 ===");

        try {
            List<ProductQnaDto> allQnas = productQnaService.getPagedQnas(1, 100, null, "createdAt");
            log.info("📊 전체 Q&A 개수: {}", allQnas.size());

            // 각 상품별 Q&A 개수 로깅
            allQnas.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            ProductQnaDto::getProductId,
                            java.util.stream.Collectors.counting()
                    ))
                    .forEach((productId, count) ->
                            log.info("🛍️ 상품 {}: {}개 Q&A", productId, count)
                    );

            return ResponseEntity.ok(allQnas);
        } catch (Exception e) {
            log.error("❌ 디버깅 Q&A 조회 실패: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // 디버깅용 테스트 엔드포인트
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("🧪 QnA Service 테스트 엔드포인트 호출됨");
        return ResponseEntity.ok("QnA Service Test OK - " + System.currentTimeMillis());
    }

    // 🔥 IP 주소 추출 유틸리티 메서드
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