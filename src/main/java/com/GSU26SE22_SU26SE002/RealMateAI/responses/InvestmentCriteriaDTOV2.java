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
public class InvestmentCriteriaDTOV2 {
    private Integer investmentCriteriaId;
    private String propertyTypeName;

    private List<ProposedPropertyDTO> proposedPropertyDTOList;
}
