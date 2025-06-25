package org.kosa.notificationservice.dto;
// 8. 카프카 메시지 DTO
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageDto {

    private Long notificationId;
    private Long broadcastId;
    private Long userId;
    private String email;
    private String type;
    private String title;
    private String message;
    private String priority;
    private LocalDateTime createdAt;

    // 방송 정보
    private String broadcastTitle;
    private String broadcasterName;
    private String thumbnailUrl;
    private LocalDateTime scheduledStartTime;
}