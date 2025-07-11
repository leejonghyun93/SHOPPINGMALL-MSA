package org.kosa.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialUserInfo {
    private String socialId;        // 소셜 플랫폼에서 제공하는 고유 ID
    private String provider;        // "kakao" 또는 "naver"
    private String email;           // 이메일
    private String name;            // 실명
    private String nickname;        // 닉네임
    private String profileImage;    // 프로필 이미지 URL
    private String gender;          // 성별 (네이버만 제공)
    private String mobile;          // 휴대폰 번호 (네이버만 제공)
}
