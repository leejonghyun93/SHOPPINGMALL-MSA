package org.kosa.livestreamingservice.client.alarm;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserServiceFeignClient {

    @GetMapping("/api/users/{userId}/email")
    UserServiceClient.UserEmailResponse getUserEmail(@PathVariable("userId") String userId);

    @GetMapping("/api/users/{userId}")
    UserServiceClient.UserInfoResponse getUserInfo(@PathVariable("userId") String userId);

//    @GetMapping("/api/users/{userId}/exists")
//    UserServiceClient.UserExistsResponse existsUser(@PathVariable("userId") String userId);
//
//    @PostMapping("/api/users/batch")
//    UserServiceClient.UserInfoBatchResponse getUserInfoBatch(@RequestBody UserServiceClient.UserIdListRequest request);
}