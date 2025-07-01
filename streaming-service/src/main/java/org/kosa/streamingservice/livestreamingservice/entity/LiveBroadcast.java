package org.kosa.streamingservice.livestreamingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tb_live_broadcasts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveBroadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "broadcast_id")
    private Long broadcastId;

    @Column(name = "broadcaster_id", nullable = false)
    private Long broadcasterId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "broadcast_status", nullable = false, length = 20)
    private String broadcastStatus = "scheduled";

    @Column(name = "scheduled_start_time", nullable = false)
    private LocalDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalDateTime scheduledEndTime;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_end_time")
    private LocalDateTime actualEndTime;

    @Column(name = "is_public")
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

    @Column(name = "category_id", length = 50)
    private String categoryId;

    @Column(name = "tags", length = 500)
    private String tags;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 연관관계
    @OneToMany(mappedBy = "broadcast", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BroadcastProduct> broadcastProducts;

    @OneToMany(mappedBy = "broadcast", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LiveBroadcastNotification> notifications;

    @OneToMany(mappedBy = "broadcast", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LiveBroadcastChat> chats;

    @OneToMany(mappedBy = "broadcast", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LiveBroadcastViewer> viewers;
}