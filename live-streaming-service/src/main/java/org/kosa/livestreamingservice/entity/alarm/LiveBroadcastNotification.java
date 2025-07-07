package org.kosa.livestreamingservice.entity.alarm;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_live_broadcast_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveBroadcastNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "broadcast_id", nullable = false)
    private Long broadcastId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "is_sent")
    private Boolean isSent = false;

    @Column(name = "priority", length = 10)
    private String priority = "NORMAL";

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // 🔧 LiveBroadcast 연관관계 제거 (독립적으로 동작)
    // broadcastId로만 참조하여 마이크로서비스 간 결합도 낮춤
}