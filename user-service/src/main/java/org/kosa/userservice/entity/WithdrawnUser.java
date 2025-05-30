package org.kosa.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawn_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawnUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userid;         // 기존 회원 아이디 (user-service PK)
    private String name;
    private String email;
    private String nickname;
    private String role;

    private String fullAddress;
    private String reason;         // 탈퇴 사유 (optional)

    private LocalDateTime createdAt;
    private LocalDateTime withdrawnAt;
}