package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposedPropertyDTO {
    private Integer proposedPropertyId;
    private Integer listingId;
    private String proposalType;
    private String propertyProjectName;
    private Integer area;
    private Double valuePrice;
    private String description;
    private FinancialMetricsDTO financialMetrics;
}