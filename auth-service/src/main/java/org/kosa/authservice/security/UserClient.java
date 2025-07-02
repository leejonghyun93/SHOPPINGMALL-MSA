package org.kosa.authservice.security;

import org.kosa.authservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * User Service와 통신하기 위한 Feign Client
 */
@FeignClient(name = "user-service", url = "${user-service.url:http://localhost:8103}")
public interface UserClient {

    /**
     * 사용자 ID로 사용자 정보 조회
     */
    @GetMapping("/api/users/{userId}")
    UserDto getUserByUserId(@PathVariable("userId") String userId);

    /**
     * 사용자 정보 검증 (비밀번호 찾기용)
     */
    @GetMapping("/api/users/verify/{userId}/{email}")
    UserDto verifyUserByUserIdAndEmail(@PathVariable("userId") String userId, @PathVariable("email") String email);

    /**
     * 비밀번호 업데이트
     */
    @PostMapping("/api/users/{userId}/password")
    void updatePassword(@PathVariable("userId") String userId, @RequestBody UpdatePasswordRequest request);

    /**
     * 비밀번호 업데이트 요청 DTO
     */
    class UpdatePasswordRequest {
        private String newPassword;

        public UpdatePasswordRequest() {}

        public UpdatePasswordRequest(String newPassword) {
            this.newPassword = newPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }
}