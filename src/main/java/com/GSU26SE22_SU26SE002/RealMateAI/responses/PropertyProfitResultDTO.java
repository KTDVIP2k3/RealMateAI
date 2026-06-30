package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả tính lợi nhuận thực tế của 1 property.
 *
 * Công thức:
 *   capitalGain = evaluatedMarketPrice - actualPurchasePrice
 *   totalRentalIncome = monthlyNetCashflow * holdingMonths
 *   totalProfitAmount = capitalGain + totalRentalIncome
 *   profitPercentage = (totalProfitAmount / actualPurchasePrice) * 100
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyProfitResultDTO {
    private Integer listingId;
    private String propertyName;
    private String usagePurpose;

    /** Giá mua ban đầu (ngày 1/1 hoặc ngày chốt) */
    private Long initialPrice;

    /** Giá thị trường hiện tại (ngày 1/6 hoặc ngày đánh giá) */
    private Long evaluatedMarketPrice;

    /** Tăng giá vốn = evaluatedMarketPrice - initialPrice */
    private Long capitalGain;

    /** Dòng tiền thuần hàng tháng = monthlyRevenue - monthlyOperatingCost */
    private Long monthlyNetCashflow;

    /** Tổng tiền thuê/khai thác tích lũy = monthlyNetCashflow * holdingMonths */
    private Long totalRentalIncome;

    /** Số tháng nắm giữ */
    private Integer holdingMonths;

    /** Tổng lợi nhuận tuyệt đối (VNĐ) = capitalGain + totalRentalIncome */
    private Long totalProfitAmount;

    /** % lợi nhuận so với vốn đầu tư ban đầu */
    private Double profitPercentage;

    /** Annualized yield (% năm) = profitPercentage / (holdingMonths / 12.0) */
    private Double annualizedYield;
}
