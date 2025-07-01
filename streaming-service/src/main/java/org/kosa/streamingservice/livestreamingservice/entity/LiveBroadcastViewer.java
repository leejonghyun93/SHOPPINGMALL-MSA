package org.kosa.streamingservice.livestreamingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_live_broadcast_viewers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiveBroadcastViewer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "viewer_id")
    private Long viewerId;

    @Column(name = "broadcast_id", nullable = false)
    private Long broadcastId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "device_type", length = 20)
    private String deviceType = "desktop";

    @CreationTimestamp
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "watch_duration")
    private Integer watchDuration = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt = LocalDateTime.now();

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id", insertable = false, updatable = false)
    private LiveBroadcast broadcast;
}
