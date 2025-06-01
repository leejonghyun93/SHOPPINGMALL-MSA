package org.kosa.userservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;


/**이벤트 DTO 클래스 생성**/
@Data
public class UserCreatedEvent {
    private String userid;
    private String email;
    private String nickname;
    private String eventType;
    private String  createdAt;
}
