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
    private String strategyName;
    private String name;
    private Long equity;
    private Long loanCapital;
    private Long currentCashflow;
    private String consciousName;
    private List<String> wardName;
    private Map<String, Object> investmentStrategyDetail;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    private List<InvestmentCriteriaDTO> investmentCriterias;
}