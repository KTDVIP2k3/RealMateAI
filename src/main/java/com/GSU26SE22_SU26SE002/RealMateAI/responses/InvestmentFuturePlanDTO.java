package com.GSU26SE22_SU26SE002.RealMateAI.responses;

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
public class InvestmentFuturePlanDTO {


    private Integer futureInvestmentPlanId;
    private String futureInvestmentPlanName;

    private Integer sourceVersionId;
    private String sourceVersionName;


    private Long equity;
    private Long loanCapital;
    private Long currentCashFlow;
    private String consciousName;
    private List<String> wardNames;
    private Integer longTermYear;
    private Integer strategyId;
    private String strategyName;
    private Map<String, Object> investmentStrategyDetail;

    private List<PropertyFutureAnalysisDTO> propertyAnalysisResults;

    private Long totalPlannedMonthlyNetCashflow;
    private Long totalActualMonthlyNetCashflow;
    private Long totalMonthlyCashflowDelta;
    private Long totalActualInvestedCapital;


    private Integer aiOverallScore;
    private String aiOverallNote;
    private String aiOverallRecommendation;
}
