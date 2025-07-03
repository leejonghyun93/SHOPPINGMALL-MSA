package org.kosa.userservice.userService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.userservice.dto.UserDto;
import org.kosa.userservice.dto.UserSessionDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserService userService;

    private static final String USER_SESSION_PREFIX = "user:session:";
    private static final long SESSION_TTL = 2 * 60 * 60; // 2시간 (초)

    /**
     * 사용자 세션 정보를 Redis에 저장
     */

    public void cacheUserSession(String userId) {
        try {
            Optional<UserDto> userOpt = userService.getMemberDetail(userId);
            if (userOpt.isPresent()) {
                UserDto user = userOpt.get();

                // JSON 문자열로 직접 생성
                String sessionJson = String.format(
                        "{\"userId\":\"%s\",\"name\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"gradeId\":\"%s\",\"status\":\"%s\",\"cachedAt\":\"%s\"}",
                        user.getUserId() != null ? user.getUserId() : "",
                        user.getName() != null ? user.getName() : "",
                        user.getEmail() != null ? user.getEmail() : "",
                        user.getPhone() != null ? user.getPhone() : "",
                        user.getGradeId() != null ? user.getGradeId() : "",
                        user.getStatus() != null ? user.getStatus() : "",
                        LocalDateTime.now().toString()
                );

                String key = USER_SESSION_PREFIX + userId;
                redisTemplate.opsForValue().set(key, sessionJson, Duration.ofSeconds(SESSION_TTL));

                log.info("✅ 사용자 세션 캐시 저장 완료: userId={}", userId);
            }
        } catch (Exception e) {
            log.error("❌ 사용자 세션 캐시 저장 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * Redis에서 사용자 세션 정보 조회
     */
    public Optional<UserSessionDto> getUserSession(String userId) {
        try {
            String key = USER_SESSION_PREFIX + userId;
            String sessionJson = (String) redisTemplate.opsForValue().get(key);

            if (sessionJson != null) {
                log.debug("✅ Redis에서 사용자 세션 조회 성공: userId={}", userId);

                // 간단한 JSON 파싱
                UserSessionDto sessionDto = parseJsonToUserSession(sessionJson);
                return Optional.ofNullable(sessionDto);
            } else {
                log.debug("⚠️ Redis에 사용자 세션이 없음: userId={}", userId);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("❌ Redis 사용자 세션 조회 실패: userId={}, error={}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    private UserSessionDto parseJsonToUserSession(String json) {
        try {
            // 간단한 JSON 파싱 (정규표현식 사용)
            String userId = extractJsonValue(json, "userId");
            String name = extractJsonValue(json, "name");
            String email = extractJsonValue(json, "email");
            String phone = extractJsonValue(json, "phone");
            String gradeId = extractJsonValue(json, "gradeId");
            String status = extractJsonValue(json, "status");
            String cachedAt = extractJsonValue(json, "cachedAt");

            return UserSessionDto.builder()
                    .userId(userId)
                    .name(name)
                    .email(email)
                    .phone(phone)
                    .gradeId(gradeId)
                    .status(status)
                    .cachedAt(cachedAt != null ? LocalDateTime.parse(cachedAt) : LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            log.error("JSON 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
    /**
     * 사용자 세션 정보 조회 (캐시 미스 시 DB 조회 후 캐시 저장)
     */
    public Optional<UserSessionDto> getUserSessionWithFallback(String userId) {
        // 1. 먼저 Redis에서 조회
        Optional<UserSessionDto> cachedSession = getUserSession(userId);
        if (cachedSession.isPresent()) {
            return cachedSession;
        }

        // 2. Redis에 없으면 DB에서 조회 후 캐시 저장
        log.info("🔄 Redis 캐시 미스 - DB에서 조회 후 캐시 저장: userId={}", userId);
        try {
            Optional<UserDto> userOpt = userService.getMemberDetail(userId);
            if (userOpt.isPresent()) {
                UserDto user = userOpt.get();

                UserSessionDto sessionDto = UserSessionDto.builder()
                        .userId(user.getUserId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .gradeId(user.getGradeId())
                        .status(user.getStatus())
                        .cachedAt(LocalDateTime.now())
                        .build();

                // 캐시에 저장
                String key = USER_SESSION_PREFIX + userId;
                redisTemplate.opsForValue().set(key, sessionDto, Duration.ofSeconds(SESSION_TTL));

                log.info("✅ DB 조회 후 캐시 저장 완료: userId={}", userId);
                return Optional.of(sessionDto);
            }
        } catch (Exception e) {
            log.error("❌ DB 조회 및 캐시 저장 실패: userId={}, error={}", userId, e.getMessage());
        }

        return Optional.empty();
    }

    /**
     * 사용자 세션 캐시 삭제 (로그아웃, 회원정보 변경 시)
     */
    public void removeUserSession(String userId) {
        try {
            String key = USER_SESSION_PREFIX + userId;
            redisTemplate.delete(key);
            log.info("✅ 사용자 세션 캐시 삭제 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("❌ 사용자 세션 캐시 삭제 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    /**
     * 사용자 정보 변경 시 캐시 갱신
     */
    public void refreshUserSession(String userId) {
        try {
            removeUserSession(userId);
            cacheUserSession(userId);
            log.info("✅ 사용자 세션 캐시 갱신 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("❌ 사용자 세션 캐시 갱신 실패: userId={}, error={}", userId, e.getMessage());
        }
    }

    private String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\":\"";
            int startIndex = json.indexOf(searchKey);
            if (startIndex == -1) return null;

            startIndex += searchKey.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) return null;

            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }
}