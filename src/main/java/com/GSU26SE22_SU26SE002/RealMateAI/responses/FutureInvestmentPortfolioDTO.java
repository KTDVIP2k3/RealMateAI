package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MỚI: DTO portfolio RIÊNG cho Future Plan (tách khỏi
 * {@link InvestmentPortfolioDTO} vốn đang dùng CHUNG với
 * InvestmentPlanServiceImplement — phương án đầu tư GỐC). Tách riêng để CHỈ
 * ẩn portfolioName ở tầng này (đã denormalize xuống tầng property lá —
 * xem FuturePortfolioAllocationPropertyDTO.portfolioName) mà KHÔNG ảnh hưởng
 * gì tới response của phương án đầu tư gốc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FutureInvestmentPortfolioDTO {
    @JsonIgnore
    private Integer investmentPortfolioId;
    @JsonIgnore
    private Integer portfolioId;

    /** Ẩn khỏi response — chỉ trả ra ở tầng property lá (properties[].portfolioName). */
    @JsonIgnore
    private String portfolioName;

    private Integer percentage;
    private Double capital;
    private List<FuturePortfolioAllocationDTO> allocations;
}
