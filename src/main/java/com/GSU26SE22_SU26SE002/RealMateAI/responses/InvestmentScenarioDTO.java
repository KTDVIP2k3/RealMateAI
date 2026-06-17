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

public class InvestmentScenarioDTO {
    @JsonIgnore
    private Integer pkInvestmentScenarioId;
    private String enumScenarioType;
    private Double decimprofitYield;
    private Double decimmonthlyCashflow;
    private Double decimprobability;
    private String textMarketNote;
    private Integer durationMonths;
    private Double decimpriceGrowthMin;
    private Double decimpriceGrowthMax;
}
