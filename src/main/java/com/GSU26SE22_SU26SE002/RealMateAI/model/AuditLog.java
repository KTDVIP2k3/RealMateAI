package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
@Entity @Table(name = "audit_log")
public class AuditLog {

    @Id @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "audit_log_id")
    private UUID auditLogId;

    private String userName;

    private String apiName;

    private String ipAddress;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "auditLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ActiveLog> activeLogs = new ArrayList<>();
}
