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
    private double interestRate;
    private double occupancyRate;
    private long monthlyPayment;
    private long monthlyCashflowIn;
    private long netCashflow;
    private long survivalCashflow;
    private long totalNetProfit;
    private double roiPercentage;
    private String riskLabel;
    private Boolean isWorthInvesting;
}