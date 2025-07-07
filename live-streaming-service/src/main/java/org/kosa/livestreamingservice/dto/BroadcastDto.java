// BroadcastDto.java
package org.kosa.livestreamingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastDto {

    // 기본 필드들
    private Long broadcastId;
    private String broadcasterId;
    private String broadcasterName;
    private String title;
    private String description;
    private String broadcastStatus;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime endTime;
    private Integer currentViewers;
    private Integer totalViewers;
    private Integer peakViewers;
    private Integer likeCount;
    private String thumbnailUrl;
    private String streamUrl;
    private Integer categoryId;
    private String categoryName;
    private String tags;
    private Boolean isPublic;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 시청자용 응답 DTO (내부 클래스)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViewerResponse {
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
        private String streamUrl;
        private Integer categoryId;
        private String categoryName;
        private String tags;
        private Boolean isPublic;
        private LocalDateTime createdAt;
    }

    // 방송 생성 요청 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String broadcasterId;
        private String title;
        private String description;
        private LocalDateTime scheduledStartTime;
        private String thumbnailUrl;
        private Integer categoryId;
        private String tags;
        private Boolean isPublic;
    }

    // 방송 목록 조회용 DTO
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private Long broadcastId;
        private String broadcasterId;
        private String broadcasterName;
        private String title;
        private String broadcastStatus;
        private LocalDateTime scheduledStartTime;
        private Integer currentViewers;
        private String thumbnailUrl;
        private String categoryName;
        private Boolean isPublic;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long broadcastId;
        private String broadcasterId;
        private String broadcasterName;
        private String title;
        private String description;
        private String broadcastStatus;
        private LocalDateTime actualStartTime;
        private Integer currentViewers;
        private Integer likeCount;
        private Integer categoryId;
        private String categoryName;
        private String tags;
        private String thumbnailUrl;
        private Integer totalViewers;
        private Integer peakViewers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlarmDto {
        private Long id;
        private String title;
        private String thumbnailUrl;
        private String broadcasterName;
        private String productName;
        private Integer salePrice;
        private String status;
        private Boolean isNotificationSet;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResponse {
        private Long broadcastId;
        private String broadcasterId;
        private String broadcasterName;
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
        private String categoryName;
        private String tags;
        private String streamKey;
        private String videoUrl;
        private String obsHost;
        private Integer obsPort;
        private String nginxHost;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScheduleResponse {
        private String time;
        private List<Response> broadcasts;
    }
}
