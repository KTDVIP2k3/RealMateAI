package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentProfileVersionDTO {
    private Integer investmentProfileVersionId;
    private Integer matchScore;
    private String strategyName;
    private String name;
    private Long equity;
    private Long loanCapital;
    private Long reserveFund;
    private String consciousName;
    private String wardName;
    private Long expectedRoi;
//    private Long minProfit;
    private String riskToleranceLevel;
    private Long durationYear;
    private LocalDate startDate;
//    private String investmentType;
    private Map<String, Object> investmentStrategyDetail;
    private List<String> legalStatus;
    private String version;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    private List<InvestmentCriteriaDTO> investmentCriterias;
}