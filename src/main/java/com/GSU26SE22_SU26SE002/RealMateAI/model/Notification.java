package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.NotificationTypeEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "notification")
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "notification_id")
    private UUID notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private NotificationTypeEnum type;

    @Column(name = "is_read")
    private Boolean isRead;

    private LocalDateTime createdAt;
}
