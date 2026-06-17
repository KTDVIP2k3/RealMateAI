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
public class UpdateInvestmentPlanRequest {
    private Integer strategy_id;
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
    private Map<String, Object> investmentStrategyDetail;
    private List<String> legalStatus;
    private List<CriteriaRequest> criteriaList;
}