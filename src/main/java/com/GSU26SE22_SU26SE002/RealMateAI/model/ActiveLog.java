package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.UserEventTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
@Entity @Table(name = "active_log")
public class ActiveLog {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "active_log_id")
    private UUID activeLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_log_id", nullable = false)
    private AuditLog auditLog;

    private String action;
    private String sessionId;
    private String ipAddress;
    private LocalDateTime createdAt;

    /** Loại sự kiện — null cho các ActiveLog không liên quan tới user-event tracking. */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 20)
    private UserEventTypeEnum eventType;

    @Column(name = "listing_id")
    private Integer listingId;
}
