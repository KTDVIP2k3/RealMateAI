package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class InvestorSurveyRequest {
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
