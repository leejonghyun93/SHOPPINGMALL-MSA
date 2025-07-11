package org.kosa.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class KakaoUserResponse {
    private Long id;

    @JsonProperty("connected_at")
    private String connectedAt;

    private Map<String, Object> properties;

    @JsonProperty("kakao_account")
    private Map<String, Object> kakaoAccount;

    // 편의 메소드들
    public String getEmail() {
        if (kakaoAccount != null) {
            return (String) kakaoAccount.get("email");
        }
        return null;
    }

    public String getNickname() {
        if (kakaoAccount != null) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                return (String) profile.get("nickname");
            }
        }
        return null;
    }

    public String getProfileImageUrl() {
        if (kakaoAccount != null) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null) {
                return (String) profile.get("profile_image_url");
            }
        }
        return null;
    }
}
