package com.GSU26SE22_SU26SE002.RealMateAI.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * FuturePortfolioAllocationProperty — mirror của
 * {@link PortfolioAllocationProperty}, link THẲNG tới
 * {@link FutureInvestmentPortfolio} (bỏ bảng trung gian PortfolioAllocation —
 * xem javadoc FutureInvestmentPortfolio). Đây là nơi lưu ĐÚNG các property
 * investor đã chọn kèm feedback thực tế (giá mua, dòng tiền, thời gian nắm
 * giữ) — GET lại phải đọc đúng từ đây, không được rỗng.
 */
@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
@Entity @Table(name = "future_portfolio_allocation_property")
public class FuturePortfolioAllocationProperty {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "future_portfolio_allocation_property_id")
    private Integer futurePortfolioAllocationPropertyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "future_investment_portfolio_id", nullable = false)
    @JsonIgnore
    private FutureInvestmentPortfolio futureInvestmentPortfolio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = true)
    @JsonIgnore
    private Property property;

    /** SYSTEM (chọn từ listing đề xuất) | MANUAL (investor tự thêm) */
    @Column(name = "property_source")
    @Builder.Default
    private String propertySource = "SYSTEM";

    /** CHO_THUE, DE_O, BAN_LAI, GIU_TAI_SAN */
    @Column(name = "usage_purpose")
    private String usagePurpose;

    /** Doanh thu hàng tháng thực tế investor nhập (VNĐ) — CÓ THỂ NULL (chưa cho thuê, để ở...) */
    @Column(name = "monthly_revenue")
    private Long monthlyRevenue;

    /** Chi phí vận hành hàng tháng (VNĐ) — CÓ THỂ NULL */
    @Column(name = "monthly_operating_cost")
    private Long monthlyOperatingCost;

    /** Giá mua thực tế investor chốt (VNĐ) — có thể khác giá listing */
    @Column(name = "actual_purchase_price")
    private Long actualPurchasePrice;

    /** Giá thị trường tại thời điểm đánh giá tương lai (VNĐ) */
    @Column(name = "evaluated_market_price")
    private Long evaluatedMarketPrice;

    /** Số tháng nắm giữ tính đến thời điểm đánh giá — lưu lại để GET tái tính CHÍNH XÁC (mặc định 6 nếu null/<=0). */
    @Column(name = "holding_months")
    private Integer holdingMonths;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
