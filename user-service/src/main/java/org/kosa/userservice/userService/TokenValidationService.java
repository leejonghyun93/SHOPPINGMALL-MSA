package org.kosa.userservice.userService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.client.AuthClient;
import org.kosa.userservice.dto.AuthResponse;
import org.kosa.userservice.dto.UserDto;
import org.kosa.userservice.dto.UserSessionDto;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    private final AuthClient authClient;
    private final UserCacheService userCacheService;

    /**
     * ✅ 개선된 토큰 검증: Auth-Service 검증 + 사용자 정보 조회
     */
    public AuthResponse validateAndGetUserInfo(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header");
            return AuthResponse.builder()
                    .success(false)
                    .message("Authorization header가 필요합니다")
                    .build();
        }

        String token = authHeader.substring(7);

        // 1. Auth-Service에서 토큰 검증
        AuthResponse authResponse = authClient.validateToken(token);

        if (!authResponse.isSuccess()) {
            log.warn("토큰 검증 실패: {}", authResponse.getMessage());
            return authResponse;
        }

        // 2. ✅ 이미 Auth-Service에서 사용자 정보를 포함해서 반환했다면 그대로 사용
        if (authResponse.getName() != null && authResponse.getEmail() != null) {
            log.debug("✅ Auth-Service에서 사용자 정보 포함 반환: userId={}", authResponse.getUserId());
            return authResponse;
        }

        // 3. ✅ Auth-Service에서 사용자 정보가 없으면 캐시/DB에서 조회
        String userId = authResponse.getUserId();
        if (userId != null) {
            Optional<UserSessionDto> sessionOpt = userCacheService.getUserSessionWithFallback(userId);
            if (sessionOpt.isPresent()) {
                UserSessionDto session = sessionOpt.get();

                return AuthResponse.builder()
                        .success(true)
                        .message("토큰이 유효합니다")
                        .userId(session.getUserId())
                        .username(session.getUserId())
                        .name(session.getName())
                        .email(session.getEmail())
                        .phone(session.getPhone())
                        .build();
            }
        }

        // 4. 사용자 정보를 찾을 수 없는 경우
        log.warn("⚠️ 토큰은 유효하지만 사용자 정보를 찾을 수 없음: userId={}", userId);
        return AuthResponse.builder()
                .success(false)
                .message("사용자 정보를 찾을 수 없습니다")
                .build();
    }

    /**
     * 토큰 검증 및 사용자 ID 추출
     */
    public String validateAndExtractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header");
            return null;
        }

        String token = authHeader.substring(7);

        // ✅ RestTemplate 방식으로 변경된 AuthClient 호출
        AuthResponse response = authClient.validateToken(token);

        if (response != null && response.isSuccess()) {
            log.debug("토큰 검증 성공: userId={}", response.getUserId());
            return response.getUserId();
        } else {
            String message = response != null ? response.getMessage() : "알 수 없는 오류";
            log.warn("토큰 검증 실패: {}", message);
            return null;
        }
    }

    /**
     * 토큰 검증 및 전체 응답 반환
     */
    public AuthResponse validateToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return AuthResponse.builder()
                    .success(false)
                    .message("Authorization header가 필요합니다")
                    .build();
        }

        String token = authHeader.substring(7);
        return authClient.validateToken(token);
    }
}