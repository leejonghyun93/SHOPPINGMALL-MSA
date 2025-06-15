package org.kosa.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {


    private String userId;           // USER_ID
    private String password;         // PASSWORD
    private String name;            // NAME
    private String email;           // EMAIL
    private String phone;           // PHONE
    private String zipcode;         // ZIPCODE
    private String address;         // ADDRESS
    private LocalDate birthDate;    // BIRTH_DATE
    private String gender;          // GENDER
    private String successionYn;    // SUCCESSION_YN
    private String blacklisted;     // BLACKLISTED
    private LocalDateTime createdDate;    // CREATED_DATE
    private LocalDateTime sessionDate;    // SESSION_DATE
    private Integer loginFailCnt;         // LOGIN_FAIL_CNT
    private String status;                // STATUS
    private LocalDateTime lastLogin;      // LAST_LOGIN
    private String marketingAgree;        // MARKETING_AGREE
    private String socialId;              // SOCIAL_ID
    private String marketingAgent;        // MARKETING_AGENT
    private String gradeId;               // GRADE_ID (FK)
    private LocalDateTime updatedDate;    // UPDATED_DATE
    private String myAddress;             // MYADDRESS
    private String secessionYn;           // SECESSION_YN
    private LocalDate secessionDate;      // SECESSION_DATE
    private String profileImg;            // PROFILE_IMG
    private String socialType;            // SOCIAL_TYPE

    // 등급 정보 (조인된 정보)
    private UserGradeDto memberGrade;

    // 편의 필드들
    private boolean accountLocked;
    private boolean isBlacklisted;
    private boolean isSeceded;
}