package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Output trả về FE sau khi tạo version FUTURE_PLAN thành công.
 * Bọc quanh InvestmentPlanDTO sẵn có (scenarios/executionPlan/portfolios) +
 * bổ sung phần kết quả tính lợi nhuận thực tế và so sánh với version gốc.
 *
 * KHÔNG có trường "status PREVIEW/SAVED" như thiết kế cũ: version được tạo
 * và lưu ngay trong 1 transaction (giống hệt updateExistingInvestmentPlan),
 * vì bản chất đây là tạo version mới chứ không phải một workflow nháp riêng.
 * Nếu sau này cần luồng "xem trước rồi mới lưu", nên xử lý ở tầng FE (giữ payload
 * tạm trong state), không cần BE lưu bản nháp xuống DB.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentFuturePlanDTO {

    /** ID version mới vừa tạo (InvestmentProfileVersion.profileVersionId) */
    private Integer newVersionId;
    private String newVersionName;

    private Integer sourceVersionId;
    private String sourceVersionName;

    // ═════════════════════════════════════════════════════════════════════
    // MỚI: "Thông tin cơ bản" cho HƯỚNG ĐI TIẾP THEO — snapshot các tham số
    // tài chính ĐÃ LƯU vào FutureInvestmentPlan (ưu tiên input investor nhập
    // mới lúc tạo, fallback sourceVersion nếu để trống). Trước đây các field
    // này CÓ lưu vào DB nhưng KHÔNG được map ra response GET.
    // ═════════════════════════════════════════════════════════════════════
    private Long equity;
    private Long loanCapital;
    private Long reserveFund;
    private String consciousName;
    private String wardName;
    private Long expectedRoi;
    private String riskToleranceLevel;
    private Long durationYear;
    private LocalDate startDate;
    private List<String> legalStatus;
    private Integer strategyId;
    private String strategyName;
    private Map<String, Object> investmentStrategyDetail;

    private List<InvestmentScenarioDTO> scenarios;
    private ExecutionPlanDTO executionPlan;
    private List<FutureInvestmentPortfolioDTO> investmentPortfolios;
    private List<PropertyProfitResultDTO> propertyProfitResults;

    private Long totalInvestedCapital;
    private Long totalMonthlyNetCashflow;
    private Long totalRentalIncomeAccumulated;
    private Long totalPortfolioProfitAmount;
    private Double totalPortfolioProfitPercentage;
    private Integer portfolioScore;

    private ComparisonSummaryDTO comparisonWithSource;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonSummaryDTO {
        private Double originalExpectedYield;
        private Double actualCalculatedYield;
        private Double yieldDelta;
        private String aiComparisonNote;
        private String aiActionRecommendation;
    }
}
