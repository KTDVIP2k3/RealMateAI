package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.InvestmentProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "investment_scenario")
public class InvestmentScenario {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investment_scenario_id")
    private Integer investmentScenarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_version_id", nullable = false)
    private InvestmentProfileVersion investmentProfileVersion;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String scenarioType;
    private Double expectedReturnRate;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
