package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyScenarioDTO {

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
}