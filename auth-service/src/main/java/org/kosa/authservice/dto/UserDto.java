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
     * 숫자로 변환 가능한 userId를 String으로 반환
     * 변환 불가능하면 null 반환 (JWT subject용)
     */
    public String getUserIdAsLong() {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        try {
            Long.parseLong(userId.trim());  // 숫자인지만 검증
            return userId.trim();           // 문자열로 반환
        } catch (NumberFormatException e) {
            // 문자열 userId인 경우 null 반환 (정상 케이스)
            return null;
        }
    }

    /**
     * 실제 Long 타입이 필요한 경우 사용
     */
    public Long getUserIdAsActualLong() {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }

        try {
            return Long.parseLong(userId.trim());  // Long 타입으로 반환
        } catch (NumberFormatException e) {
            // 문자열 userId인 경우 null 반환
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
        return getUserIdAsActualLong() != null;
    }

    /**
     * 사용자가 문자열 ID를 가지고 있는지 확인
     */
    public boolean hasStringUserId() {
        return !hasNumericUserId() && userId != null && !userId.trim().isEmpty();
    }
}