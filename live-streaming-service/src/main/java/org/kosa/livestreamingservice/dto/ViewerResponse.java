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
public class ViewerResponse {
    private Long broadcastId;
    private String broadcasterId;
    private String broadcasterName;
    private String title;
    private String description;
    private String broadcastStatus;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime actualStartTime;
    private Integer currentViewers;
    private Integer totalViewers;
    private Integer peakViewers;
    private Integer likeCount;
    private String thumbnailUrl;

    // 스트림 관련 필드들 추가
    private String streamUrl;        // HLS URL
    private String streamKey;        // OBS 설정용
    private String nginxHost;        // Docker 서버 IP
    private String obsHost;          // OBS PC IP
    private Integer obsPort;         // OBS WebSocket 포트

    private Integer categoryId;
    private String categoryName;
    private String tags;
    private Boolean isPublic;
    private LocalDateTime createdAt;
}