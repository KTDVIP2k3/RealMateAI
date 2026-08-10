package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAnalyticsDto {
    @JsonProperty("total_revenue")
    private BigDecimal totalRevenue;

    private String currency = "VND";

    private RevenueBreakdownDto breakdown;

    @JsonProperty("chart_data")
    private List<RevenueChartDataDto> chartData;
}