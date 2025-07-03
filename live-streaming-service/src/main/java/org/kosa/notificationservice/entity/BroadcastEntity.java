package org.kosa.notificationservice.entity;

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
    private Long broadcasterId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    // 🔥 String으로 변경 (enum 문제 해결)
    @Column(name = "broadcast_status")
    private String broadcastStatus;

    @Column(name = "scheduled_start_time")
    private LocalDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalDateTime scheduledEndTime;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "stream_url")
    private String streamUrl;

    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "tags")
    private String tags;

    @Column(name = "current_viewers")
    private Integer currentViewers;

    @Column(name = "like_count")
    private Integer likeCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}