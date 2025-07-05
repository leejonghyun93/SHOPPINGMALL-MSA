package org.kosa.userservice.userController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.WithdrawRequestDto;
import org.kosa.userservice.dto.WithdrawResponseDto;
import org.kosa.userservice.userService.TokenValidationService;
import org.kosa.userservice.userService.UserWithdrawalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/users/withdrawal")
@RequiredArgsConstructor
@Slf4j
public class UserWithdrawalController {

    private final UserWithdrawalService userWithdrawalService;
    private final TokenValidationService tokenValidationService;

    /**
     * 회원탈퇴 처리
     * URL: POST /api/users/withdrawal/process
     */
    @PostMapping("/process")
    public ResponseEntity<?> withdrawUser(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        try {
            log.info("회원탈퇴 요청 시작");

            // Authorization 헤더에서 토큰 추출
            String authenticatedUserId = tokenValidationService.validateAndExtractUserId(
                    httpRequest.getHeader("Authorization")
            );

            if (authenticatedUserId == null) {
                log.warn("토큰 검증 실패 - 회원탈퇴");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "인증이 필요합니다."));
            }

            // 요청 데이터 추출
            String userId = (String) request.get("userId");
            String password = (String) request.get("password");
            String withdrawalReason = (String) request.get("withdrawalReason");
            String withdrawalDateStr = (String) request.get("withdrawalDate");

            log.info("회원탈퇴 요청 데이터 - userId: {}, reason: {}, date: {}",
                    userId, withdrawalReason, withdrawalDateStr);

            // 입력값 검증
            if (userId == null || userId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "사용자 ID가 필요합니다."));
            }

            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "비밀번호가 필요합니다."));
            }

            if (withdrawalReason == null || withdrawalReason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "탈퇴 사유가 필요합니다."));
            }

            // 요청 사용자와 인증된 사용자 일치 확인
            if (!authenticatedUserId.equals(userId)) {
                log.warn("User ID mismatch - URL: {}, Token: {}", userId, authenticatedUserId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "본인만 탈퇴 처리가 가능합니다."));
            }

            // 날짜 파싱
            LocalDate withdrawalDate = null;
            if (withdrawalDateStr != null && !withdrawalDateStr.trim().isEmpty()) {
                try {
                    withdrawalDate = LocalDate.parse(withdrawalDateStr);
                } catch (Exception e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "올바른 날짜 형식이 아닙니다."));
                }
            } else {
                withdrawalDate = LocalDate.now();
            }

            // WithdrawRequestDto 생성
            WithdrawRequestDto withdrawRequest = WithdrawRequestDto.builder()
                    .userId(userId)
                    .password(password)
                    .withdrawalReason(withdrawalReason)
                    .withdrawalDate(withdrawalDate)
                    .build();

            log.info("회원탈퇴 서비스 호출 - withdrawRequest: {}", withdrawRequest);

            // 회원탈퇴 서비스 호출
            WithdrawResponseDto response = userWithdrawalService.withdrawUser(withdrawRequest);

            if (response.isSuccess()) {
                log.info("회원탈퇴 처리 완료: userId={}, withdrawnId={}", userId, response.getWithdrawnId());
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", response.getMessage(),
                        "withdrawnId", response.getWithdrawnId()
                ));
            } else {
                log.warn("회원탈퇴 처리 실패: userId={}, message={}", userId, response.getMessage());
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", response.getMessage()));
            }

        } catch (Exception e) {
            log.error("회원탈퇴 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "서버 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }


    /**
     * 서버 상태 확인용 엔드포인트
     */
    @GetMapping("/health")
    public ResponseEntity<?> withdrawalHealthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "service", "UserWithdrawalController",
                "timestamp", java.time.LocalDateTime.now(),
                "message", "회원탈퇴 서비스가 정상 동작 중입니다."
        ));
    }


}