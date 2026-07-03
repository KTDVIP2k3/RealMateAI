package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.InvestmentProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "execution_plan")
public class ExecutionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_plan_id")
    private Integer executionPlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_version_id", nullable = false)
    private InvestmentProfileVersion investmentProfileVersion;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int match_score;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
