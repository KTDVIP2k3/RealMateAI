package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestorDTO {
    private Integer surveyId;
    private String investmentStyle;
    private String investmentExperience;
    private String profitTarget;
    private String managementAbility;
    private String levelOfVolatility;
    private String capitalUtilizationMindset;
    private String positionalPriority;
    private String investmentMethod;
    private Boolean stableIncome;
}
