package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialMetricsDTO {
    private Long estimatedProfit;
//    private Long estimatedPriceGrowth;
    private Long monthlyRentalCashflow;
    private Long monthlyPrincipalInterest;
    private Long netCashflow;
    private Double roiPercentage;
}