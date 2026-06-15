package com.GSU26SE22_SU26SE002.RealMateAI.responses;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionPlanDTO {
    @JsonIgnore
    private Integer pkExecutionPlanId;
    private Double totalInvestmentCapital;
    private Double decimloanPercentage;
    private Double decimmonthlyPayment;
    private Double decimprobability;
    private Double decimnetCashflow;
    private Integer maxHoldingMonths;
    private Boolean booleanIsLegalClear;
    private Boolean booleanIsLeverageSafe;
    private String stringLiquidityDurationRange;
    private Boolean booleanIsReserveFundEnough;
    private String textTakeProfitStrategy;
    private String textHoldingTimeLimit;
    private String textQuickSellAction;
}
