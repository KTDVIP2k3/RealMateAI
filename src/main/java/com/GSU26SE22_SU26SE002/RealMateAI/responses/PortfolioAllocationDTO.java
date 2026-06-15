package com.GSU26SE22_SU26SE002.RealMateAI.responses;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAllocationDTO {
    @JsonIgnore
    private Integer pkPortfolioAllocationId;
    @JsonIgnore
    private Integer fkPropertyTypeId;
    private String propertyTypeName;

    private List<PortfolioAllocationPropertyDTO> properties;
}
