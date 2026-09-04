package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "property_scenario")
public class PropertyScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_scenario_id")
    private Integer propertyScenarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposed_property_id")
    @JsonIgnore
    private ProposedProperty proposedProperty;

    private String scenarioType;
    private Double interestRate;
    private Double occupancyRate;
    private Long monthlyPayment;
    private Long monthlyCashflowIn;
    private Long netCashflow;
    private Long survivalCashflow;
    private Long totalNetProfit;
    private Double roiPercentage;
    private String riskLabel;
    private Boolean isWorthInvesting;
    private LocalDateTime createdAt;
}