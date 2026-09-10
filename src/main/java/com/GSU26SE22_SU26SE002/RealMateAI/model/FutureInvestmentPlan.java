package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
@Entity @Table(name = "future_investment_plan")
public class FutureInvestmentPlan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "future_investment_plan_id")
    private Integer futureInvestmentPlanId;

    /** Tên tự sinh: "Kết quả dự đoán 1", "Kết quả dự đoán 2"... (đếm theo investmentProfile) — hoặc investor tự đặt qua planName. */
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_profile_id", nullable = false)
    @JsonIgnore
    private InvestmentProfile investmentProfile;

    /** Version InvestmentPlan GỐC (BÌNH THƯỜNG, không thể là 1 FutureInvestmentPlan khác) làm baseline so sánh. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_version_id", nullable = false)
    @JsonIgnore
    private InvestmentProfileVersion sourceVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    @JsonIgnore
    private Strategy strategy;

    private Long equity;
    private Long loanCapital;
    private Long currentCashflow;
    private String conscious;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "wards", columnDefinition = "json")
    private List<String> wards;

    private Integer longTermYear;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "investment_strategy_detail", columnDefinition = "json")
    private Map<String, Object> investmentStrategyDetail;

    /**
     * MỚI: snapshot ĐẦY ĐỦ kết quả phân tích (propertyAnalysisResults +
     * aiOverallScore/Note/Recommendation) — lưu JSON, đọc lại nguyên vẹn khi
     * GET, KHÔNG tính lại / KHÔNG gọi lại AI. Thay thế hoàn toàn khái niệm
     * "profitSummary" cũ (chỉ lưu số tổng hợp) — giờ lưu ĐỦ chi tiết từng
     * property luôn, vì Future Plan không còn nặng như trước (không có
     * scenarios 3-kịch-bản/portfolio phức tạp).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_snapshot", columnDefinition = "json")
    private Map<String, Object> analysisSnapshot;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
