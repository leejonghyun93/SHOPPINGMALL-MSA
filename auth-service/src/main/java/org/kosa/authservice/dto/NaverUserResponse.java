package org.kosa.authservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class NaverUserResponse {
    @JsonProperty("resultcode")
    private String resultCode;

    private String message;

    private Map<String, Object> response;

    // 편의 메소드들
    public String getId() {
        if (response != null) {
            return (String) response.get("id");
        }
        return null;
    }

    public String getEmail() {
        if (response != null) {
            return (String) response.get("email");
        }
        return null;
    }

    public String getName() {
        if (response != null) {
            return (String) response.get("name");
        }
        return null;
    }

    public String getNickname() {
        if (response != null) {
            return (String) response.get("nickname");
        }
        return null;
    }

    public String getProfileImage() {
        if (response != null) {
            return (String) response.get("profile_image");
        }
        return null;
    }

    public String getGender() {
        if (response != null) {
            return (String) response.get("gender");
        }
        return null;
    }

    public String getMobile() {
        if (response != null) {
            return (String) response.get("mobile");
        }
        return null;
    }
}