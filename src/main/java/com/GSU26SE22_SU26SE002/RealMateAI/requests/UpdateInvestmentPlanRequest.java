package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvestmentPlanRequest {

    private Integer strategyId;
    private Long equity;
    private Long loanCapital;
    private Long currentCashFlow;
    private String consciousName;
    private List<String> wardNames;
    private Integer holdingMonths;
    private Map<String, Object> investmentStrategyDetail;
    private List<CriteriaRequest> criteriaList;
}