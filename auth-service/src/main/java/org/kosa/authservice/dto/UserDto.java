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
    private String userId;   // USER_ID

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

    public String getUsername() {
        return this.userId;
    }

    public Long getUserIdAsLong() {
        try {
            return Long.parseLong(this.userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}