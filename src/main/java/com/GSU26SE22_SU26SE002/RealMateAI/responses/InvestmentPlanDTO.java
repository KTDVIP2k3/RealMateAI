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
    private Integer investmentProfileId;
    private Integer investmentProfileVersionId;
    private Long totalCapital;
    private Long loanCapital;
    private Long equity;
    private String strategyName;
    private List<InvestmentCriteriaDTOV2> investmentCriteriaDTOV2s;
}
