package org.kosa.userservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    @Id
    @Column(name = "USER_ID", length = 50)
    private String userId;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "EMAIL", length = 100)
    private String email;

    @Column(name = "PHONE", length = 20)
    private String phone;

    @Column(name = "ZIPCODE", length = 10)
    private String zipcode;

    @Column(name = "ADDRESS", length = 500)
    private String address;

    @Column(name = "BIRTH_DATE")
    private LocalDate birthDate;

    @Column(name = "GENDER", length = 1, columnDefinition = "CHAR(1)")
    @Builder.Default
    private String gender = "U"; //  기본값 설정

    @Column(name = "SUCCESSION_YN", length = 1, columnDefinition = "CHAR(1)")
    @Builder.Default
    private String successionYn = "N";

    @Column(name = "BLACKLISTED", length = 1, columnDefinition = "CHAR(1)")
    @Builder.Default
    private String blacklisted = "N";

    @CreationTimestamp
    @Column(name = "CREATED_DATE", nullable = false)
    private LocalDateTime createdDate;
    @Column(name = "nickname")
    private String nickname;  //  추가 필요


    @Column(name = "SESSION_DATE")
    private LocalDateTime sessionDate;

    @Column(name = "LOGIN_FAIL_CNT")
    @Builder.Default
    private Integer loginFailCnt = 0;

    @Column(name = "STATUS", length = 20)
    @Builder.Default
    private String status = "Y";

    @Column(name = "LAST_LOGIN")
    private LocalDateTime lastLogin;

    @Column(name = "MARKETING_AGREE", length = 1, columnDefinition = "CHAR(1)")
    @Builder.Default
    private String marketingAgree = "N";

    @Column(name = "SOCIAL_ID", length = 100)
    private String socialId;

    @Column(name = "MARKETING_AGENT", length = 100)
    private String marketingAgent;

    @UpdateTimestamp
    @Column(name = "UPDATED_DATE")
    private LocalDateTime updatedDate;

    @Column(name = "MYADDRESS", length = 200)
    private String myAddress;

    @Column(name = "SECESSION_YN", length = 1, columnDefinition = "CHAR(1)")
    @Builder.Default
    private String secessionYn = "N";

    @Column(name = "SECESSION_DATE")
    private LocalDate secessionDate;

    @Column(name = "PROFILE_IMG", length = 255)
    private String profileImg;

    @Column(name = "SOCIAL_TYPE", length = 50)
    private String socialType;

    // 외래키 관계 - 기본 등급 설정 (BRONZE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GRADE_ID", foreignKey = @ForeignKey(name = "FK_MEMBER_GRADE"))
    private MemberGrade memberGrade;

    // JPA 생명주기 메서드로 기본값 설정
    @PrePersist
    public void prePersist() {
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "ACTIVE";
        }
        if (this.loginFailCnt == null) {
            this.loginFailCnt = 0;
        }
        if (this.gender == null) {
            this.gender = "U";
        }
        if (this.marketingAgree == null) {
            this.marketingAgree = "N";
        }
        if (this.successionYn == null) {
            this.successionYn = "N";
        }
        if (this.blacklisted == null) {
            this.blacklisted = "N";
        }
        if (this.secessionYn == null) {
            this.secessionYn = "N";
        }
    }

    // 편의 메서드
    public boolean isAccountLocked() {
        return loginFailCnt != null && loginFailCnt >= 5;
    }

    public boolean isBlacklisted() {
        return "Y".equals(blacklisted);
    }

    public boolean isSeceded() {
        return "Y".equals(secessionYn);
    }

    // 기본 등급 설정 메서드
    public void setDefaultGrade(MemberGrade defaultGrade) {
        if (this.memberGrade == null) {
            this.memberGrade = defaultGrade;
        }
    }
}