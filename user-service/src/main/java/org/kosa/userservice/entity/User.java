package org.kosa.userservice.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "userid", nullable = false, unique = true, length = 50)
    private String userid;

    @Column(name = "passwd", nullable = false, length = 100)
    private String passwd;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "age")
    private int age;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "detail_address", length = 200)
    private String detailAddress;

    @Column(name = "full_address", length = 400)
    @JsonProperty("fullAddress")
    private String fullAddress;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "reg_date")
    private LocalDateTime regDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "role", length = 20)
    @Builder.Default
    private String role = "USER";  // 기본값 설정

    @Column(name = "login_fail_count")
    private int loginFailCount;  // 기본값 설정

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "account_locked")
    @Builder.Default
    private Boolean accountLocked = false;  // 기본값 설정

    public boolean isAccountLocked() {
        return Boolean.TRUE.equals(accountLocked);
    }

    /**
     * 엔티티가 저장되기 전에 호출되는 메서드
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (this.regDate == null) {
            this.regDate = now;
        }

        if (this.updateDate == null) {
            this.updateDate = now;
        }

        if (this.loginTime == null) {
            this.loginTime = now;
        }

        if (this.role == null) {
            this.role = "USER";
        }

        if (this.accountLocked == null) {
            this.accountLocked = false;
        }

//        if (this.loginFailCount == 0) {
//            this.loginFailCount = 0;
//        }
    }

    /**
     * 엔티티가 업데이트되기 전에 호출되는 메서드
     */
    @PreUpdate
    public void preUpdate() {
        this.updateDate = LocalDateTime.now();
    }
}