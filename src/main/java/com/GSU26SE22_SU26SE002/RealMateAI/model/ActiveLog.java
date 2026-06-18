package com.GSU26SE22_SU26SE002.RealMateAI.model;

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
}
