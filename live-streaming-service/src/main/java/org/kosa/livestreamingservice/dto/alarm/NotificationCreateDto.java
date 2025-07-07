package org.kosa.livestreamingservice.dto.alarm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;



@Data
public class NotificationCreateDto {

    @NotNull(message = "방송 ID는 필수입니다")
    private Long broadcastId;

    @NotNull(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotBlank(message = "알림 타입은 필수입니다")
    private String type; // BROADCAST_START, BROADCAST_END, BROADCAST_REMINDER

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    private String message;

    private String priority = "NORMAL"; // LOW, NORMAL, HIGH, URGENT
}