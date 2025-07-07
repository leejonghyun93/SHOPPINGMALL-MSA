package org.kosa.authservice.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kosa.authservice.dto.*;
import org.kosa.authservice.security.AuthResponse;
import org.kosa.authservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SocialAuthService {

    @Value("${user-service.url:http://localhost:8103}")
    private String userServiceUrl;

    @Value("${social.kakao.client-id:}")
    private String kakaoClientId;

    @Value("${social.kakao.client-secret:}")
    private String kakaoClientSecret;

    @Value("${social.kakao.redirect-uri:}")
    private String kakaoRedirectUri;

    @Value("${social.naver.client-id:}")
    private String naverClientId;

    @Value("${social.naver.client-secret:}")
    private String naverClientSecret;

    @Value("${social.naver.redirect-uri:}")
    private String naverRedirectUri;

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;

    /**
     * 애플리케이션 시작 시 설정값 로그 출력
     */
    @PostConstruct
    public void logConfiguration() {
        log.info("🔧 소셜 로그인 설정 확인:");
        log.info("  📁 User Service URL: {}", userServiceUrl);
        log.info("  🟡 카카오 Client ID: {}", maskSensitiveData(kakaoClientId));
        log.info("  🟡 카카오 Client Secret: {}", maskSensitiveData(kakaoClientSecret));
        log.info("  🟡 카카오 Redirect URI: {}", kakaoRedirectUri);
        log.info("  🟢 네이버 Client ID: {}", maskSensitiveData(naverClientId));
        log.info("  🟢 네이버 Client Secret: {}", maskSensitiveData(naverClientSecret));
        log.info("  🟢 네이버 Redirect URI: {}", naverRedirectUri);
    }

    private String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) return "❌ 설정되지 않음";
        if (data.length() <= 8) return data.substring(0, 2) + "***";
        return data.substring(0, 4) + "***" + data.substring(data.length() - 4);
    }

    /**
     * 소셜 로그인 처리 메인 메소드
     */
    public AuthResponse processSocialLogin(String code, String state) {
        try {
            log.info("🔍 소셜 로그인 처리 시작 - code: {}, state: {}",
                    code != null ? code.substring(0, Math.min(code.length(), 10)) + "..." : null, state);

            // 카카오와 네이버 모두 시도해보기 (실제로는 프론트엔드에서 구분해서 보내는 것이 좋음)

            // 1. 카카오 로그인 시도
            log.info("🟡 카카오 로그인 먼저 시도");
            AuthResponse kakaoResult = processKakaoLogin(code);
            if (kakaoResult.isSuccess()) {
                log.info("✅ 카카오 로그인 성공");
                return kakaoResult;
            }

            // 2. 네이버 로그인 시도
            log.info("🟢 카카오 실패, 네이버 로그인 시도");
            AuthResponse naverResult = processNaverLogin(code, state);
            if (naverResult.isSuccess()) {
                log.info("✅ 네이버 로그인 성공");
                return naverResult;
            }

            log.warn("⚠️ 모든 소셜 로그인 시도 실패");
            return AuthResponse.builder()
                    .success(false)
                    .message("소셜 로그인 처리 중 오류가 발생했습니다.")
                    .build();

        } catch (Exception e) {
            log.error("💥 소셜 로그인 처리 중 예외 발생", e);
            return AuthResponse.builder()
                    .success(false)
                    .message("소셜 로그인 처리 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 카카오 로그인 처리
     */
    private AuthResponse processKakaoLogin(String code) {
        try {
            log.info("🟡 카카오 로그인 처리 시작");

            // 1. Access Token 발급
            KakaoTokenResponse tokenResponse = getKakaoAccessToken(code);
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                log.warn("❌ 카카오 토큰 발급 실패");
                return AuthResponse.builder()
                        .success(false)
                        .message("카카오 인증에 실패했습니다.")
                        .build();
            }

            log.info("✅ 카카오 토큰 발급 성공");

            // 2. 사용자 정보 조회
            KakaoUserResponse userResponse = getKakaoUserInfo(tokenResponse.getAccessToken());
            if (userResponse == null || userResponse.getId() == null) {
                log.warn("❌ 카카오 사용자 정보 조회 실패");
                return AuthResponse.builder()
                        .success(false)
                        .message("카카오 사용자 정보 조회에 실패했습니다.")
                        .build();
            }

            log.info("✅ 카카오 사용자 정보 조회 성공 - id: {}, email: {}, nickname: {}",
                    userResponse.getId(), userResponse.getEmail(), userResponse.getNickname());

            // 3. 소셜 사용자 정보 객체 생성
            SocialUserInfo socialUser = SocialUserInfo.builder()
                    .socialId("kakao_" + userResponse.getId())
                    .provider("kakao")
                    .email(userResponse.getEmail())
                    .name(userResponse.getNickname()) // 카카오는 실명을 제공하지 않으므로 닉네임 사용
                    .nickname(userResponse.getNickname())
                    .profileImage(userResponse.getProfileImageUrl())
                    .build();

            // 4. 사용자 생성 또는 업데이트 후 JWT 토큰 발급
            return processUserCreationOrUpdate(socialUser);

        } catch (Exception e) {
            log.error("💥 카카오 로그인 처리 중 오류", e);
            return AuthResponse.builder()
                    .success(false)
                    .message("카카오 로그인 처리 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 네이버 로그인 처리
     */
    private AuthResponse processNaverLogin(String code, String state) {
        try {
            log.info("🟢 네이버 로그인 처리 시작");

            // 1. Access Token 발급
            NaverTokenResponse tokenResponse = getNaverAccessToken(code, state);
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                log.warn("❌ 네이버 토큰 발급 실패");
                return AuthResponse.builder()
                        .success(false)
                        .message("네이버 인증에 실패했습니다.")
                        .build();
            }

            log.info("✅ 네이버 토큰 발급 성공");

            // 2. 사용자 정보 조회
            NaverUserResponse userResponse = getNaverUserInfo(tokenResponse.getAccessToken());
            if (userResponse == null || userResponse.getId() == null) {
                log.warn("❌ 네이버 사용자 정보 조회 실패");
                return AuthResponse.builder()
                        .success(false)
                        .message("네이버 사용자 정보 조회에 실패했습니다.")
                        .build();
            }

            log.info("✅ 네이버 사용자 정보 조회 성공 - id: {}, email: {}, name: {}",
                    userResponse.getId(), userResponse.getEmail(), userResponse.getName());

            // 3. 소셜 사용자 정보 객체 생성
            SocialUserInfo socialUser = SocialUserInfo.builder()
                    .socialId("naver_" + userResponse.getId())
                    .provider("naver")
                    .email(userResponse.getEmail())
                    .name(userResponse.getName())
                    .nickname(userResponse.getNickname())
                    .profileImage(userResponse.getProfileImage())
                    .gender(userResponse.getGender())
                    .mobile(userResponse.getMobile())
                    .build();

            // 4. 사용자 생성 또는 업데이트 후 JWT 토큰 발급
            return processUserCreationOrUpdate(socialUser);

        } catch (Exception e) {
            log.error("💥 네이버 로그인 처리 중 오류", e);
            return AuthResponse.builder()
                    .success(false)
                    .message("네이버 로그인 처리 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 카카오 Access Token 발급
     */
    private KakaoTokenResponse getKakaoAccessToken(String code) {
        try {
            log.debug("🔍 카카오 토큰 발급 요청 시작");

            // 🔥 상세 파라미터 로그 추가
            log.info("📋 카카오 토큰 요청 파라미터:");
            log.info("  - grant_type: authorization_code");
            log.info("  - client_id: {}", maskSensitiveData(kakaoClientId));
            log.info("  - client_secret: {}", maskSensitiveData(kakaoClientSecret));
            log.info("  - redirect_uri: {}", kakaoRedirectUri);
            log.info("  - code: {}...", code != null ? code.substring(0, Math.min(code.length(), 10)) : "null");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", kakaoClientId);
            params.add("client_secret", kakaoClientSecret);
            params.add("redirect_uri", kakaoRedirectUri);
            params.add("code", code);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            log.info("🚀 카카오 토큰 API 호출: https://kauth.kakao.com/oauth/token");

            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                    "https://kauth.kakao.com/oauth/token", request, KakaoTokenResponse.class);

            log.info("📥 카카오 토큰 API 응답 - Status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("✅ 카카오 토큰 발급 성공");
                KakaoTokenResponse tokenResponse = response.getBody();
                log.info("  - access_token: {}...",
                        tokenResponse.getAccessToken() != null ?
                                tokenResponse.getAccessToken().substring(0, Math.min(20, tokenResponse.getAccessToken().length())) : "null");
                return tokenResponse;
            }

            log.warn("❌ 카카오 토큰 발급 실패 - HTTP Status: {}", response.getStatusCode());
            return null;

        } catch (HttpClientErrorException e) {
            log.error("💥 카카오 토큰 발급 중 HTTP 오류 - Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("💥 카카오 토큰 발급 중 오류", e);
            return null;
        }
    }

    /**
     * 네이버 Access Token 발급
     */
    private NaverTokenResponse getNaverAccessToken(String code, String state) {
        try {
            log.debug("🔍 네이버 토큰 발급 요청 시작");

            // 🔥 상세 파라미터 로그 추가
            log.info("📋 네이버 토큰 요청 파라미터:");
            log.info("  - grant_type: authorization_code");
            log.info("  - client_id: {}", maskSensitiveData(naverClientId));
            log.info("  - client_secret: {}", maskSensitiveData(naverClientSecret));
            log.info("  - redirect_uri: {}", naverRedirectUri);
            log.info("  - code: {}...", code != null ? code.substring(0, Math.min(code.length(), 10)) : "null");
            log.info("  - state: {}", state);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("grant_type", "authorization_code");
            params.add("client_id", naverClientId);
            params.add("client_secret", naverClientSecret);
            params.add("redirect_uri", naverRedirectUri);
            params.add("code", code);
            params.add("state", state);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            log.info("🚀 네이버 토큰 API 호출: https://nid.naver.com/oauth2.0/token");

            ResponseEntity<NaverTokenResponse> response = restTemplate.postForEntity(
                    "https://nid.naver.com/oauth2.0/token", request, NaverTokenResponse.class);

            log.info("📥 네이버 토큰 API 응답 - Status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("✅ 네이버 토큰 발급 성공");
                NaverTokenResponse tokenResponse = response.getBody();
                log.info("  - access_token: {}...",
                        tokenResponse.getAccessToken() != null ?
                                tokenResponse.getAccessToken().substring(0, Math.min(20, tokenResponse.getAccessToken().length())) : "null");
                return tokenResponse;
            }

            log.warn("❌ 네이버 토큰 발급 실패 - HTTP Status: {}", response.getStatusCode());
            return null;

        } catch (HttpClientErrorException e) {
            log.error("💥 네이버 토큰 발급 중 HTTP 오류 - Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("💥 네이버 토큰 발급 중 오류", e);
            return null;
        }
    }

    /**
     * 카카오 사용자 정보 조회
     */
    private KakaoUserResponse getKakaoUserInfo(String accessToken) {
        try {
            log.debug("🔍 카카오 사용자 정보 조회 시작");
            log.info("🚀 카카오 사용자 정보 API 호출: https://kapi.kakao.com/v2/user/me");
            log.info("  - Authorization: Bearer {}...", accessToken.substring(0, Math.min(20, accessToken.length())));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<KakaoUserResponse> response = restTemplate.exchange(
                    "https://kapi.kakao.com/v2/user/me", HttpMethod.GET, request, KakaoUserResponse.class);

            log.info("📥 카카오 사용자 정보 API 응답 - Status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("✅ 카카오 사용자 정보 조회 성공");
                return response.getBody();
            }

            log.warn("❌ 카카오 사용자 정보 조회 실패 - HTTP Status: {}", response.getStatusCode());
            return null;

        } catch (HttpClientErrorException e) {
            log.error("💥 카카오 사용자 정보 조회 중 HTTP 오류 - Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("💥 카카오 사용자 정보 조회 중 오류", e);
            return null;
        }
    }

    /**
     * 네이버 사용자 정보 조회
     */
    private NaverUserResponse getNaverUserInfo(String accessToken) {
        try {
            log.debug("🔍 네이버 사용자 정보 조회 시작");
            log.info("🚀 네이버 사용자 정보 API 호출: https://openapi.naver.com/v1/nid/me");
            log.info("  - Authorization: Bearer {}...", accessToken.substring(0, Math.min(20, accessToken.length())));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);

            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<NaverUserResponse> response = restTemplate.exchange(
                    "https://openapi.naver.com/v1/nid/me", HttpMethod.GET, request, NaverUserResponse.class);

            log.info("📥 네이버 사용자 정보 API 응답 - Status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("✅ 네이버 사용자 정보 조회 성공");
                return response.getBody();
            }

            log.warn("❌ 네이버 사용자 정보 조회 실패 - HTTP Status: {}", response.getStatusCode());
            return null;

        } catch (HttpClientErrorException e) {
            log.error("💥 네이버 사용자 정보 조회 중 HTTP 오류 - Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("💥 네이버 사용자 정보 조회 중 오류", e);
            return null;
        }
    }

    /**
     * 🔥 소셜 사용자 생성 또는 업데이트 후 JWT 토큰 발급 (이름 포함)
     */
    private AuthResponse processUserCreationOrUpdate(SocialUserInfo socialUser) {
        try {
            log.info("🔍 소셜 사용자 처리 시작 - provider: {}, socialId: {}, name: '{}', nickname: '{}'",
                    socialUser.getProvider(), socialUser.getSocialId(), socialUser.getName(), socialUser.getNickname());

            // User Service에 소셜 사용자 정보 전송
            UserDto user = createOrUpdateUserInUserService(socialUser);

            if (user == null) {
                log.error("❌ User Service에서 사용자 생성/업데이트 실패");
                return AuthResponse.builder()
                        .success(false)
                        .message("사용자 정보 처리 중 오류가 발생했습니다.")
                        .build();
            }

            log.info("✅ 소셜 사용자 처리 성공 - userId: {}, name: '{}'", user.getUserId(), user.getName());

            // 🔥 실제 이름을 찾아서 토큰에 포함
            String actualName = determineActualName(user, socialUser);

            log.info("🔍 최종 결정된 이름: '{}'", actualName);

            // 🔥 이름을 포함하여 JWT 토큰 생성
            String accessToken = jwtUtil.generateToken(user.getUserId(), "USER", actualName);

            log.info("✅ 소셜 로그인 완료 - userId: {}, provider: {}, name: '{}'",
                    user.getUserId(), socialUser.getProvider(), actualName);

            return AuthResponse.builder()
                    .success(true)
                    .message("소셜 로그인 성공")
                    .token(accessToken)
                    .userId(user.getUserId())
                    .username(user.getUserId())
                    .name(actualName) // 🔥 실제 이름을 응답에도 포함
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build();

        } catch (Exception e) {
            log.error("💥 소셜 사용자 처리 중 오류", e);
            return AuthResponse.builder()
                    .success(false)
                    .message("사용자 정보 처리 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 🔥 실제 사용할 이름 결정 (우선순위: DB 이름 → 소셜 이름 → 닉네임)
     */
    private String determineActualName(UserDto user, SocialUserInfo socialUser) {
        log.info("🔍 이름 결정 시작 - DB name: '{}', Social name: '{}', Social nickname: '{}'",
                user.getName(), socialUser.getName(), socialUser.getNickname());

        // 1. 소셜에서 받은 실제 이름이 있으면 최우선 사용
        if (socialUser.getName() != null && !socialUser.getName().trim().isEmpty()) {
            String socialName = socialUser.getName().trim();
            if (!socialName.equals("소셜사용자") &&
                    !socialName.equals("사용자") &&
                    !socialName.equals(user.getUserId()) &&
                    socialName.length() >= 2) {
                log.info("🔍 소셜 실제 이름 사용: '{}'", socialName);
                return socialName;
            }
        }

        // 2. 소셜 닉네임이 있으면 사용
        if (socialUser.getNickname() != null && !socialUser.getNickname().trim().isEmpty()) {
            String nickname = socialUser.getNickname().trim();
            if (!nickname.equals("소셜사용자") &&
                    !nickname.equals("사용자") &&
                    !nickname.equals(user.getUserId()) &&
                    nickname.length() >= 2) {
                log.info("🔍 소셜 닉네임 사용: '{}'", nickname);
                return nickname;
            }
        }

        // 3. DB에서 가져온 이름이 유효하면 사용
        if (user.getName() != null && !user.getName().trim().isEmpty()) {
            String dbName = user.getName().trim();
            if (!dbName.equals("소셜사용자") &&
                    !dbName.equals("사용자") &&
                    !dbName.equals(user.getUserId()) &&
                    dbName.length() >= 2) {
                log.info("🔍 DB 이름 사용: '{}'", dbName);
                return dbName;
            }
        }

        // 4. 제공업체별 기본값
        String provider = socialUser.getProvider();
        String providerName;
        switch (provider.toUpperCase()) {
            case "KAKAO":
                providerName = "카카오사용자";
                break;
            case "NAVER":
                providerName = "네이버사용자";
                break;
            case "GOOGLE":
                providerName = "구글사용자";
                break;
            default:
                providerName = "소셜사용자";
        }

        log.info("🔍 제공업체 기본값 사용: '{}'", providerName);
        return providerName;
    }

    /**
     * User Service에 소셜 사용자 생성/업데이트 요청
     */
    private UserDto createOrUpdateUserInUserService(SocialUserInfo socialUser) {
        try {
            String url = userServiceUrl + "/api/users/social";

            log.debug("🔍 User Service 소셜 사용자 요청: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 소셜 사용자 정보를 Map으로 변환
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("socialId", socialUser.getSocialId());
            requestBody.put("provider", socialUser.getProvider());
            requestBody.put("email", socialUser.getEmail());
            requestBody.put("name", socialUser.getName());
            requestBody.put("nickname", socialUser.getNickname());
            requestBody.put("profileImage", socialUser.getProfileImage());
            requestBody.put("gender", socialUser.getGender());
            requestBody.put("mobile", socialUser.getMobile());

            log.info("📤 User Service 요청 데이터: provider={}, name='{}', nickname='{}'",
                    socialUser.getProvider(), socialUser.getName(), socialUser.getNickname());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<UserDto> response = restTemplate.postForEntity(url, request, UserDto.class);

            log.info("📥 User Service 응답 - Status: {}", response.getStatusCode());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                UserDto responseUser = response.getBody();
                log.info("✅ User Service 소셜 사용자 처리 성공 - userId: {}, name: '{}'",
                        responseUser.getUserId(), responseUser.getName());
                return responseUser;
            } else {
                log.error("❌ User Service 소셜 사용자 처리 실패 - Status: {}", response.getStatusCode());
                return null;
            }

        } catch (HttpClientErrorException e) {
            log.error("💥 User Service 요청 중 HTTP 오류 - Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("💥 User Service 소셜 사용자 요청 중 오류", e);
            return null;
        }
    }
}