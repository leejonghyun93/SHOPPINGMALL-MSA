package org.kosa.userservice.userController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.WithdrawRequestDto;
import org.kosa.userservice.dto.WithdrawResponseDto;
import org.kosa.userservice.entity.WithdrawnMember;
import org.kosa.userservice.userService.TokenValidationService;
import org.kosa.userservice.userService.UserWithdrawalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserWithdrawalController {

    private final UserWithdrawalService userWithdrawalService;
    private final TokenValidationService tokenValidationService;

    /**
     * 회원탈퇴 처리
     */
    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawResponseDto> withdrawUser(
            @Valid @RequestBody WithdrawRequestDto request,
            HttpServletRequest httpRequest) {

        try {
            // 🔥 순수 JWT 방식: Authorization 헤더에서만 토큰 추출
            String authenticatedUserId = tokenValidationService.validateAndExtractUserId(
                    httpRequest.getHeader("Authorization")
            );

            if (authenticatedUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(WithdrawResponseDto.builder()
                                .success(false)
                                .message("인증이 필요합니다.")
                                .build());
            }

            // 요청 사용자와 인증된 사용자 일치 확인
            if (!authenticatedUserId.equals(request.getUserId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(WithdrawResponseDto.builder()
                                .success(false)
                                .message("본인만 탈퇴 처리가 가능합니다.")
                                .build());
            }

            WithdrawResponseDto response = userWithdrawalService.withdrawUser(request);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("회원탈퇴 API 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(WithdrawResponseDto.builder()
                            .success(false)
                            .message("서버 오류가 발생했습니다.")
                            .build());
        }
    }

    /**
     * 탈퇴 회원 조회 (관리자용)
     */
    @GetMapping("/withdrawn/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawnMember> getWithdrawnMember(@PathVariable String userId) {
        return userWithdrawalService.getWithdrawnMember(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 기간별 탈퇴 통계 (관리자용)
     */
    @GetMapping("/withdrawn/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WithdrawnMember>> getWithdrawalStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<WithdrawnMember> statistics = userWithdrawalService
                .getWithdrawnMembersByDateRange(startDate, endDate);

        return ResponseEntity.ok(statistics);
    }
}