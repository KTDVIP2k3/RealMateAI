package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "investment_profile_version")
public class InvestmentProfileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_version_id")
    private Integer profileVersionId;

    private String profileVersionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_profile_id")
    @JsonIgnore
    private InvestmentProfile investmentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    @JsonIgnore
    private Strategy strategy;

    private Long equity;
    private Long loanCapital;
    private Long currentCashflow;
    private String conscious;

    private Long totalCapital;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "wards", columnDefinition = "json")
    private List<String> wards;

//    private Long expectedRoi;
    private Long expectedProfit;
    private Integer holdingMonths;
//    private String riskToleranceLevel;
//    private Long durationYear;
//    private LocalDate startDate;
//    private String investmentType;

//    private int match_score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "investment_strategy_detail", columnDefinition = "json")
    private Map<String, Object> investmentStrategyDetail;

//    private String legalStatus;
    private String version;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_version_id")
    @JsonIgnore
    private InvestmentProfileVersion baseVersion;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profit_summary", columnDefinition = "json")
    private Map<String, Object> profitSummary;

    @OneToMany(mappedBy = "investmentProfileVersion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvestmentCriteria> investmentCriterias = new ArrayList<>();

//    @OneToMany(mappedBy = "investmentProfileVersion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Builder.Default
//    @JsonIgnore
//    private List<InvestmentScenario> investmentScenarios = new ArrayList<>();

//    @OneToMany(mappedBy = "investmentProfileVersion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Builder.Default
//    @JsonIgnore
//    private List<InvestmentPortfolio> investmentPortfolios = new ArrayList<>();
//
//    @OneToMany(mappedBy = "investmentProfileVersion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//    @Builder.Default
//    @JsonIgnore
//    private List<ExecutionPlan> executionPlans = new ArrayList<>();
}