package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * MODIFIED:
 * Future Plan KHÔNG phải một entity riêng — nó CHÍNH LÀ một InvestmentProfileVersion mới,
 * giống hệt cách updateExistingInvestmentPlan() đã tạo version mới khi investor đổi tham số vốn.
 * Khác biệt duy nhất: nguồn dữ liệu input đến từ feedback thực tế của investor (đã chọn property,
 * đã có giá mua thực, đã có dòng tiền thực) thay vì từ form khảo sát ban đầu.
 *
 * Do đó chỉ cần thêm 3 field tối thiểu để:
 *  (a) đánh dấu version này được sinh ra theo nguồn nào (planSourceType)
 *  (b) trỏ về version baseline để so sánh / truy vết lịch sử (baseVersion)
 *  (c) lưu kết quả tính lợi nhuận TỔNG HỢP cấp version (vì đây là số liệu tổng hợp toàn danh mục,
 *      không phải số liệu của riêng 1 property — nên không hợp lý nếu nhét vào
 *      PortfolioAllocationProperty, và cũng không cần bảng riêng vì nó 1-1 với version)
 *
 * field "version" và "investmentType" có sẵn nhưng đang luôn NULL trong code cũ —
 * field "version" được tái sử dụng làm planSourceType, tránh phình thêm cột vô ích.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@Table(name = "investment_profile_version")
public class InvestmentProfileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_version_id")
    private Integer profileVersionId;

    private String profileVersionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_profile_id")
    private InvestmentProfile investmentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    private Long equity;
    private Long loanCapital;
    private Long reserveFund;
    private String conscious;
    private String ward;
    private Long expectedRoi;
    private Long minProfit;
    private String riskToleranceLevel;
    private Long durationYear;
    private LocalDate startDate;
    private String investmentType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "investment_strategy_detail", columnDefinition = "json")
    private Map<String, Object> investmentStrategyDetail;

    private String legalStatus;

    /**
     * MODIFIED (tái sử dụng field có sẵn, trước đây luôn NULL):
     * Đánh dấu nguồn sinh ra version này:
     *  - "AI_GENERATED": version tạo từ luồng AI Stage1-3 thông thường (generateCompleteInvestmentPlan / updateExistingInvestmentPlan)
     *  - "FUTURE_PLAN": version tạo từ feedback thực tế của investor (luồng mới)
     * NULL = mặc định coi như AI_GENERATED (tương thích ngược dữ liệu cũ).
     */
    private String version;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * NEW: Trỏ về version gốc dùng làm baseline khi version này là FUTURE_PLAN.
     * NULL nếu đây là version AI_GENERATED thông thường (root version).
     * Dùng để truy vết "version tương lai này sinh ra từ version nào" và để so sánh ROI kỳ vọng vs thực tế.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_version_id")
    private InvestmentProfileVersion baseVersion;

    /**
     * NEW: Kết quả tính lợi nhuận THỰC TẾ tổng hợp toàn danh mục, chỉ có giá trị khi version = FUTURE_PLAN.
     * Lý do để JSON thay vì cột riêng: đây là số liệu tổng hợp (tổng vốn, tổng dòng tiền, %lợi nhuận,
     * điểm AI...) chỉ dùng để hiển thị, không cần query/filter theo từng field riêng lẻ ở tầng SQL.
     * Chi tiết breakdown TỪNG property thì đã nằm sẵn trong PortfolioAllocationProperty (KHÔNG lặp lại ở đây).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profit_summary", columnDefinition = "json")
    private Map<String, Object> profitSummary;

    @OneToMany(mappedBy = "investmentProfileVersion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvestmentCriteria> investmentCriterias = new ArrayList<>();

    @OneToMany(mappedBy = "investmentProfileVersion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvestmentScenario> investmentScenarios = new ArrayList<>();

    @OneToMany(mappedBy = "investmentProfileVersion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<InvestmentPortfolio> investmentPortfolios = new ArrayList<>();

    @OneToMany(mappedBy = "investmentProfileVersion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ExecutionPlan> executionPlans = new ArrayList<>();
}