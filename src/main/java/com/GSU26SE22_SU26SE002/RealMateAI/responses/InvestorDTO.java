package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestorDTO {
    private Integer surveyId;
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
