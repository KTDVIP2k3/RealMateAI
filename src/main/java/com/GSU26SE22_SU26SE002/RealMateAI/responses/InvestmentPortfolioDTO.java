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
public class InvestmentPortfolioDTO {
    @JsonIgnore
    private Integer investmentPortfolioId;
    @JsonIgnore
    private Integer portfolioId;
    private String portfolioName;
    private Integer percentage;
    private Double capital;
    private List<PortfolioAllocationDTO> allocations;
}
