package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.InvestmentPlanDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaveInvestmentPlanRequest {
    private InvestmentPlanRequest inputRequest;
    private InvestmentPlanDTO aiOutputData;
}