package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PropertyFutureAnalysisDTO {

    private Integer listingId;
    private String propertyProjectName;

    private Long plannedMonthlyRentalCashflow;
    private Long plannedNetCashflow;
    private Double plannedRoiPercentage;
    private Long plannedEstimatedProfit;

    private String usagePurpose;
    private Long actualPurchasePrice;
    private Long actualMonthlyRevenue;
    private Long actualMonthlyOperatingCost;
    private Long actualMonthlyNetCashflow;
    private Integer actualHoldingMonths;

    private Double actualAnnualCashflowYieldPercentage;

    private Long monthlyCashflowDelta;

    private String aiAnalysisNote;

    private String aiActionRecommendation;
}
