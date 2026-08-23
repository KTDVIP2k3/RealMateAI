package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentPlanRequest {
    private String name;
    private Long equity;
    private Long loanCapital;
    private Long reserveFund;
    private Long currentCashFlow;
    private String consciousName;
    private List<String> wardNames;
    private Long expectedRoi;
    private String riskToleranceLevel;
    private Long durationYear;
    private LocalDate startDate;
    private List<String> legalStatus;
    private Integer strategyId;
    private Map<String, Object> investmentStrategyDetail;
    private List<CriteriaRequest> criteriaList;
}