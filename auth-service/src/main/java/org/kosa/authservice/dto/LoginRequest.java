package org.kosa.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @JsonProperty("userid")   // JSON에서는 userid로 받음
    private String userId;    // 내부적으로는 userId 사용

    @JsonProperty("passwd")   // JSON에서는 passwd로 받음
    private String password;  // 내부적으로는 password 사용
}