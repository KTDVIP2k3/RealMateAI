package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * FE gửi để tạo 1 {@link com.GSU26SE22_SU26SE002.RealMateAI.model.FutureInvestmentPlan}
 * mới (bảng riêng, tách khỏi InvestmentProfileVersion) từ feedback thực tế của investor.
 *
 * Không tồn tại bảng "feedback" riêng: mỗi item ở đây map 1-1 vào 1 row
 * FuturePortfolioAllocationProperty mới được tạo cho future-plan mới này.
 * Tương tự cách processStage2EnrichProperties() ghi PortfolioAllocationPropertyDTO,
 * chỉ khác nguồn dữ liệu là investor nhập tay thay vì AI truy vấn warehouse.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateFuturePlanRequest {

    /** Version gốc (InvestmentProfileVersion) làm baseline để clone tham số + so sánh ROI */
    private Integer sourceVersionId;

    /** Tên version mới investor đặt (optional, tự sinh nếu null) */
    private String planName;

    /** Danh sách property investor đã chọn, kèm thông số sử dụng thực tế */
    private List<SelectedPropertyItem> selectedProperties;

    // ═════════════════════════════════════════════════════════════════════
    // MỚI: "Thông tin cơ bản" cho HƯỚNG ĐI TIẾP THEO của kế hoạch — investor
    // có thể nhập lại tham số tài chính MỚI (khác sourceVersion) để AI phân
    // tích 3 kịch bản dựa trên định hướng MỚI này, thay vì chỉ lặp lại y hệt
    // số liệu cũ. MỌI field đều OPTIONAL — để trống (null) thì giữ nguyên
    // hành vi cũ: clone y hệt giá trị tương ứng từ sourceVersion. Cùng field
    // set với InvestmentPlanRequest (tạo phương án gốc) để đồng bộ ý nghĩa.
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
    private Map<String, Object> investmentStrategyDetail;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SelectedPropertyItem {
        /** ID Listing hệ thống đề xuất. NULL nếu propertySource = MANUAL */
        private Integer listingId;

        /** SYSTEM (đề xuất AI) | MANUAL (investor tự thêm ngoài đề xuất) */
        private String propertySource;

        /** Bắt buộc nếu propertySource = MANUAL (vì không có listingId để tra Property) */
        private Integer manualPropertyId;

        /** Mục đích sử dụng: CHO_THUE | DE_O | BAN_LAI | GIU_TAI_SAN */
        private String usagePurpose;

        /** Doanh thu hàng tháng thực tế (VNĐ) */
        private Long monthlyRevenue;

        /** Chi phí vận hành hàng tháng (VNĐ) */
        private Long monthlyOperatingCost;

        /** Giá mua thực tế sẽ chốt (VNĐ) — input để tính capital gain */
        private Long actualPurchasePrice;

        /** Giá thị trường tại thời điểm đánh giá, vd ngày 1/6 (VNĐ) — input để tính capital gain */
        private Long evaluatedMarketPrice;

        /** Số tháng nắm giữ tính đến thời điểm đánh giá */
        private Integer holdingMonths;

        /** Portfolio (danh mục) mà property này thuộc về, để gắn đúng PortfolioAllocation */
        private Integer portfolioId;
    }
}
