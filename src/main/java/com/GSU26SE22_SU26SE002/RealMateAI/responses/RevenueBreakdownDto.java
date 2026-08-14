package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueBreakdownDto {
    @JsonProperty("posting_package_revenue")
    private BigDecimal postingPackageRevenue;

    @JsonProperty("membership_revenue")
    private BigDecimal membershipRevenue;
}