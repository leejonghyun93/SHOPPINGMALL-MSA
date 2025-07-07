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
public class BroadcastDto {
    private Long id;
    private String broadcasterId;
    private String title;
    private String description;
    private String broadcastStatus;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private Boolean isPublic;
    private Integer maxViewers;
    private Integer currentViewers;
    private Integer totalViewers;
    private Integer peakViewers;
    private Integer likeCount;
    private String thumbnailUrl;
    private String streamUrl;
    private Integer categoryId;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String streamKey;

    // 새로 추가된 필드들
    private String videoUrl;
    private String obsHost;
    private Integer obsPort;
    private String obsPassword;
    private String nginxHost;

    // 기존 필드들 (화면 표시용)
    private String broadcasterName;
    private String productName;
    private Integer salePrice;
    private String status;
    private Boolean isNotificationSet;
}