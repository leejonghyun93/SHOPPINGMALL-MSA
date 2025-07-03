package org.kosa.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 사용자 세션 캐시 정보
 * Redis에 저장될 사용자 기본 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;      // 사용자 ID
    private String name;        // 실제 이름
    private String email;       // 이메일
    private String phone;       // 전화번호
    private String role;        // 권한
    private LocalDateTime lastAccess; // 마지막 접근 시간

    /**
     * 기본 정보만 포함하는 생성자
     */
    public UserSessionInfo(String userId, String name, String email, String phone, String role) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.lastAccess = LocalDateTime.now();
    }
}