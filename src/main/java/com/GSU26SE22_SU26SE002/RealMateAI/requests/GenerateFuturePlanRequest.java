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
public class GenerateFuturePlanRequest {

    private Integer sourceVersionId;

    private String planName;

    private Long equity;
    private Long loanCapital;
    private Long currentCashFlow;
    private String consciousName;
    private List<String> wardNames;
    private Integer longTermYear;
    private Integer strategyId;
    private Map<String, Object> investmentStrategyDetail;

    private List<SelectedPropertyItem> selectedProperties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedPropertyItem {
        private Integer listingId;

        private String propertySource;

        private Integer manualPropertyId;

        private String usagePurpose;

        private Long monthlyRevenue;

        private Long monthlyOperatingCost;

        private Long actualPurchasePrice;

        private Integer holdingMonths;

    }
}
