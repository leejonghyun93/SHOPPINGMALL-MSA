// BroadcastEntity.java
package org.kosa.livestreamingservice.entity.alarm;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_live_broadcasts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BroadcastEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broadcast_id")
    private Long broadcastId;

    @Column(name = "broadcaster_id")
    private String broadcasterId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "broadcast_status")
    private String broadcastStatus;

    @Column(name = "scheduled_start_time")
    private LocalDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalDateTime scheduledEndTime;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(name = "is_public")
    private Boolean isPublic;

    @Column(name = "max_viewers")
    private Integer maxViewers;

    @Column(name = "current_viewers")
    private Integer currentViewers;

    @Column(name = "total_viewers")
    private Integer totalViewers;

    @Column(name = "peak_viewers")
    private Integer peakViewers;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "stream_url")
    private String streamUrl;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "tags")
    private String tags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "stream_key")
    private String streamKey;

    // 새로 추가된 컬럼들
    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "obs_host")
    private String obsHost;

    @Column(name = "obs_port")
    private Integer obsPort;

    @Column(name = "obs_password")
    private String obsPassword;

    @Column(name = "nginx_host")
    private String nginxHost;
}