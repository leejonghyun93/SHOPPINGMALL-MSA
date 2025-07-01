package org.kosa.streamingservice.notificationservice.entity;

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

    @Column(name = "broadcaster_id", length = 50, nullable = false)
    private String broadcasterId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "broadcast_status", length = 20, nullable = false)
    private String broadcastStatus = "scheduled";

    @Column(name = "scheduled_start_time", nullable = false)
    private LocalDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalDateTime scheduledEndTime;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Column(name = "max_viewers")
    private Integer maxViewers = 0;

    @Column(name = "current_viewers")
    private Integer currentViewers = 0;

    @Column(name = "total_viewers")
    private Integer totalViewers = 0;

    @Column(name = "peak_viewers")
    private Integer peakViewers = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "stream_url", length = 500)
    private String streamUrl;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "stream_key", length = 500)
    private String streamKey;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}