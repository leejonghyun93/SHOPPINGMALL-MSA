package org.kosa.livestreamingservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_broadcast_status_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "broadcast_id", nullable = false)
    private Long broadcastId;

    @Column(name = "previous_status", length = 20)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 20)
    private String newStatus;

    @Column(name = "changed_by")
    private Long changedBy;

    @Column(name = "change_reason", length = 255)
    private String changeReason;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id", insertable = false, updatable = false)
    private LiveBroadcast broadcast;
}