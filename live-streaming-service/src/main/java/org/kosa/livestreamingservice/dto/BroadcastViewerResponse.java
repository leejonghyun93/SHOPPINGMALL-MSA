// BroadcastViewerResponse.java (별도 파일)
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
public class BroadcastViewerResponse {
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