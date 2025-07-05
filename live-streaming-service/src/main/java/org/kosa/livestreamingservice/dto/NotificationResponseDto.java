package org.kosa.livestreamingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Long notificationId;
    private Long broadcastId;
    private String userId;
    private String type;
    private String title;
    private String message;
    private Boolean isRead;
    private Boolean isSent;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    // 추가 정보 (방송 정보)
    private String broadcastTitle;
    private String broadcasterName;
    private String thumbnailUrl;
    private LocalDateTime updatedAt;
}