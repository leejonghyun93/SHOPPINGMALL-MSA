package org.kosa.livestreamingservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberEntity {

    @Id
    @Column(name = "USER_ID")
    private String userId;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "NAME")
    private String name;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "PHONE")
    private String phone;

    @Column(name = "ZIPCODE")
    private String zipcode;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;

    @Column(name = "GENDER", columnDefinition = "CHAR(1)")
    private String gender;

    @Column(name = "SUCCESSION_YN", columnDefinition = "CHAR(1)")
    @Builder.Default
    private String successionYn = "N";

    @Column(name = "BLACKLISTED", columnDefinition = "CHAR(1)")
    @Builder.Default
    private String blacklisted = "N";

    @Column(name = "CREATED_DATE")
    private LocalDateTime createdDate;

    @Column(name = "SESSION_DATE")
    private LocalDateTime sessionDate;

    @Column(name = "LOGIN_FAIL_CNT")
    @Builder.Default
    private Integer loginFailCnt = 0;

    @Column(name = "STATUS", columnDefinition = "CHAR(1)")
    @Builder.Default
    private String status = "Y";

    @Column(name = "LAST_LOGIN")
    private LocalDateTime lastLogin;

    @Column(name = "MARKETING_AGREE", columnDefinition = "CHAR(1)")
    @Builder.Default
    private String marketingAgree = "N";

    @Column(name = "SOCIAL_ID")
    private String socialId;

    @Column(name = "MARKETING_AGENT")
    private String marketingAgent;

    @Column(name = "GRADE_ID")
    private String gradeId;

    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @Column(name = "MYADDRESS")
    private String myAddress;

    @Column(name = "SECESSION_YN", columnDefinition = "CHAR(1)")
    @Builder.Default
    private String secessionYn = "N";

    @Column(name = "SECESSION_DATE")
    private LocalDate secessionDate;

    @Column(name = "PROFILE_IMG")
    private String profileImg;

    @Column(name = "SOCIAL_TYPE")
    private String socialType;

    @Column(name = "nickname")
    private String nickname;
}