package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class InvestorSurveyRequest {
    private String investmentExperience;
    private Boolean stableIncome;
    private String investmentGoal;
    private String investmentPriority;
    private String investmentStyle;
    private String returnExpectation;
    private String propertyPreference;
    private String decisionFactor;
    private String managementAbility;
    private String investmentMethod;
}
