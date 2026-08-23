package com.GSU26SE22_SU26SE002.RealMateAI.responses;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentPlanDTO {
    private Integer score;
    private Long totalCapital;
//    private List<InvestmentPortfolioDTO> investmentPortfolios;
    private List<InvestmentCriteriaDTOV2> investmentCriteriaDTOV2s;
    private List<InvestmentScenarioDTO> scenarios;
//    private ExecutionPlanDTO executionPlan;
}
