package org.kosa.userservice.userController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "회원 탈퇴", description = "회원 탈퇴 처리 API")
@RestController
@RequestMapping("/api/users/withdrawal")
@RequiredArgsConstructor
@Slf4j
public class UserWithdrawalController {

    private final UserWithdrawalService userWithdrawalService;
    private final TokenValidationService tokenValidationService;

    @Operation(summary = "회원탈퇴 처리", description = "사용자 계정을 탈퇴 처리합니다")
    @SecurityRequirement(name = "JWT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "탈퇴 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    @PostMapping("/process")
    public ResponseEntity<?> withdrawUser(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {

        try {
            String authenticatedUserId = tokenValidationService.validateAndExtractUserId(
                    httpRequest.getHeader("Authorization")
            );

            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("success", false, "message", "인증이 필요합니다."));
            }

            String userId = (String) request.get("userId");
            String password = (String) request.get("password");
            String withdrawalReason = (String) request.get("withdrawalReason");
            String withdrawalDateStr = (String) request.get("withdrawalDate");

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

            if (!authenticatedUserId.equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "본인만 탈퇴 처리가 가능합니다."));
            }

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

            WithdrawRequestDto withdrawRequest = WithdrawRequestDto.builder()
                    .userId(userId)
                    .password(password)
                    .withdrawalReason(withdrawalReason)
                    .withdrawalDate(withdrawalDate)
                    .build();

            WithdrawResponseDto response = userWithdrawalService.withdrawUser(withdrawRequest);

            if (response.isSuccess()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", response.getMessage(),
                        "withdrawnId", response.getWithdrawnId()
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", response.getMessage()));
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "message", "서버 오류가 발생했습니다: " + e.getMessage()
                    ));
        }
    }

    @Operation(summary = "서비스 상태 확인", description = "회원탈퇴 서비스 상태를 확인합니다")
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