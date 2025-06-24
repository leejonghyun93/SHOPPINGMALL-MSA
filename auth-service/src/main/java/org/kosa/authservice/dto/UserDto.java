package org.kosa.authservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDto {

    @JsonProperty("userId")  // JSON의 userId 필드와 매핑
    private String userId;   // USER_ID (문자열로 처리)

    @JsonProperty("password")  // JSON의 password 필드와 매핑
    private String password; // PASSWORD

    @JsonProperty("name")
    private String name;     // NAME

    @JsonProperty("email")
    private String email;    // EMAIL

    @JsonProperty("phone")
    private String phone;    // PHONE

    @JsonProperty("zipcode")
    private String zipcode;  // ZIPCODE

    @JsonProperty("address")
    private String address;  // ADDRESS

    @JsonProperty("birthDate")
    private LocalDate birthDate; // BIRTH_DATE

    @JsonProperty("gender")
    private String gender;   // GENDER

    @JsonProperty("status")
    private String status;   // STATUS

    @JsonProperty("createdDate")
    private LocalDateTime createdDate; // CREATED_DATE

    @JsonProperty("lastLogin")
    private LocalDateTime lastLogin; // LAST_LOGIN

    @JsonProperty("gradeId")
    private String gradeId;

    @JsonProperty("marketingAgree")
    private String marketingAgree;

    @JsonProperty("socialType")
    private String socialType;

    @JsonProperty("profileImg")
    private String profileImg;

    @JsonProperty("secessionYn")
    private String secessionYn;

    /**
     * username은 userId와 동일
     */
    public String getUsername() {
        return this.userId;
    }

    /**
     * userId를 Long으로 변환 시도 (실패하면 null 반환)
     * 문자열 userId인 경우 null을 반환하며, 이는 정상적인 동작임
     */
    public Long getUserIdAsLong() {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(userId.trim());
        } catch (NumberFormatException e) {
            // 문자열 userId인 경우 null 반환 (정상 케이스)
            return null;
        }
    }

    /**
     * 사용자 식별자 반환 (숫자든 문자열이든)
     */
    public String getUserIdentifier() {
        return this.userId;
    }

    /**
     * 사용자가 숫자 ID를 가지고 있는지 확인
     */
    public boolean hasNumericUserId() {
        return getUserIdAsLong() != null;
    }

    /**
     * 사용자가 문자열 ID를 가지고 있는지 확인
     */
    public boolean hasStringUserId() {
        return !hasNumericUserId() && userId != null && !userId.trim().isEmpty();
    }
}