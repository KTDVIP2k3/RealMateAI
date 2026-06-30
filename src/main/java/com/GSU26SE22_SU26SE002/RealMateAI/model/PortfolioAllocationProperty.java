package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * MODIFIED: Thêm các trường hỗ trợ:
 *  - usagePurpose: mục đích sử dụng property (cho thuê, để ở, bán lại...)
 *  - monthlyRevenue / monthlyOperatingCost: dòng tiền thực tế investor nhập
 *  - actualPurchasePrice: giá mua thực tế (có thể thấp hơn giá listing)
 *  - evaluatedMarketPrice: giá thị trường tại thời điểm đánh giá (ngày 1/6)
 *  - propertySource: SYSTEM (đề xuất AI) | MANUAL (investor tự thêm)
 *  - isSelected: investor đã chọn property này hay chưa (dùng cho future plan)
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "portfolio_allocation_property")
public class PortfolioAllocationProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_allocation_property_id")
    private Integer portfolioAllocationPropertyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_allocation_id", nullable = false)
    private PortfolioAllocation portfolioAllocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    /** Tỉ trọng phân bổ trong danh mục (0.0 -> 1.0) */
    private Double weight;

    /**
     * Nguồn property:
     *  - SYSTEM: AI đề xuất từ kho hàng (queryRealWarehouseProperties)
     *  - MANUAL: Investor tự chọn thêm ngoài danh sách đề xuất
     */
    @Column(name = "property_source")
    @Builder.Default
    private String propertySource = "SYSTEM";

    /**
     * Mục đích sử dụng property:
     *  - CHO_THUE, DE_O, BAN_LAI, GIU_TAI_SAN
     * Được nhập bởi investor trong flow "Chọn & Cập nhật" (Step 2)
     */
    @Column(name = "usage_purpose")
    private String usagePurpose;

    /** Doanh thu hàng tháng thực tế investor nhập (VNĐ) */
    @Column(name = "monthly_revenue")
    private Long monthlyRevenue;

    /** Chi phí vận hành hàng tháng (VNĐ) */
    @Column(name = "monthly_operating_cost")
    private Long monthlyOperatingCost;

    /** Giá mua thực tế investor chốt (VNĐ) — có thể khác giá listing */
    @Column(name = "actual_purchase_price")
    private Long actualPurchasePrice;

    /**
     * Giá thị trường tại thời điểm đánh giá tương lai (VNĐ).
     * Dùng trong hàm tính lợi nhuận cuối kỳ (ngày 1/6 so với ngày 1/1).
     */
    @Column(name = "evaluated_market_price")
    private Long evaluatedMarketPrice;

    /** Investor đã bấm "Chọn căn này" hay chưa */
    @Column(name = "is_selected")
    private Boolean isSelected;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
