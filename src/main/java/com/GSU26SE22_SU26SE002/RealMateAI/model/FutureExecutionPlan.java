package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
@Entity @Table(name = "future_execution_plan")
public class FutureExecutionPlan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "future_execution_plan_id")
    private Integer futureExecutionPlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "future_investment_plan_id", nullable = false)
    private FutureInvestmentPlan futureInvestmentPlan;

    private String name;

    /** JSON serialize của ExecutionPlanDTO (giữ nguyên cách làm cũ — parse lại lúc GET) */
    @Column(columnDefinition = "TEXT")
    private String description;

    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
