package org.kosa.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {
    private String code;
    private String state;
    private String provider; // "kakao" 또는 "naver"
}