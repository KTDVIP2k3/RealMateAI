package com.GSU26SE22_SU26SE002.RealMateAI.responses;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MỚI: DTO property lá RIÊNG cho Future Plan (tách khỏi
 * {@link PortfolioAllocationPropertyDTO} vốn đang dùng CHUNG với
 * InvestmentPlanServiceImplement — phương án đầu tư GỐC, không liên quan tới
 * Future Plan). Tách riêng để thêm field portfolioName mà KHÔNG ảnh hưởng gì
 * tới response của phương án đầu tư gốc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuturePortfolioAllocationPropertyDTO {
    private Integer portfolioAllocationPropertyId;
    private String propertyProjectName;
    private Integer area;
    private Double valuePrice;
    private String description;
    private String propertySource;
    private String portfolioName;
}
