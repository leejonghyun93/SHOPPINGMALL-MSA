package org.kosa.userservice.entity;


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
    private String role;  // ex) USER, ADMIN

    @Column(name = "login_fail_count")
    private int loginFailCount;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "account_locked")
    private Boolean accountLocked;

    public boolean isAccountLocked() {
        return Boolean.TRUE.equals(accountLocked);
    }
}
