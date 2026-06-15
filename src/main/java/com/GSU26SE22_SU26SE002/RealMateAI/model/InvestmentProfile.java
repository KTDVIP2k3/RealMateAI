package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "investment_profile")
public class InvestmentProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investment_profile_id")
    private Integer investmentProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id")
    private Investor investor;


    /** Strategy -> InvestmentProfile (one-to-many in ERD) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    private String name;
    private Long equity;
    private Long loanCapital;
    private Long reserveFund;
    private String conscious;
    private String ward;
    private Long expectedRoi;
    private Long minProfit;
    private String riskToleranceLevel;
    private Long durationYear;
    private LocalDate startDate;
    private String investmentType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "investment_strategy_detail", columnDefinition = "json")
    private Map<String, Object> investmentStrategyDetail;
    private String legalStatus;
    private String version;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "investmentProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvestmentCriteria> investmentCriterias = new ArrayList<>();

    @OneToMany(mappedBy = "investmentProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvestmentScenario> investmentScenarios = new ArrayList<>();

    @OneToMany(mappedBy = "investmentProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvestmentPortfolio> investmentPortfolios = new ArrayList<>();

    @OneToMany(mappedBy = "investmentProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ExecutionPlan> executionPlans = new ArrayList<>();
}
