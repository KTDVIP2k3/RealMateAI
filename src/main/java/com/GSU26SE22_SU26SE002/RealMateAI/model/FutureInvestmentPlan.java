package com.GSU26SE22_SU26SE002.RealMateAI.model;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * FutureInvestmentPlan — bảng HOÀN TOÀN RIÊNG BIỆT cho "Kế hoạch tương lai",
 * KHÔNG còn tái sử dụng InvestmentProfileVersion (version="FUTURE_PLAN") như
 * thiết kế cũ nữa.
 *
 * Quan hệ:
 *  - {@link #investmentProfile}: Investment Profile (CHA) đã tạo ra kế hoạch
 *    tương lai này — GIỮ NGUYÊN như thiết kế cũ (vẫn lưu lại profile nào sinh
 *    ra future-plan này).
 *  - {@link #sourceVersion}: version GỐC (BÌNH THƯỜNG) làm baseline so sánh.
 *    Field này khai báo kiểu {@link InvestmentProfileVersion} — do
 *    InvestmentProfileVersion không còn đại diện cho future-plan nữa (future
 *    plan đã tách khỏi bảng này), nên KHÔNG THỂ trỏ field này vào 1
 *    FutureInvestmentPlan khác được nữa → "không tạo future từ future" được
 *    đảm bảo NGAY Ở TẦNG THIẾT KẾ DỮ LIỆU, không cần check thủ công.
 *
 * Version riêng: mỗi FutureInvestmentPlan là 1 bản ghi ĐỘC LẬP, không có khái
 * niệm "version number" nối tiếp nhau như InvestmentProfileVersion — nhiều
 * FutureInvestmentPlan có thể cùng trỏ về 1 sourceVersion (đại diện nhiều lần
 * investor cập nhật feedback thực tế khác nhau từ CÙNG 1 kế hoạch gốc).
 */
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
    private InvestmentProfile investmentProfile;

    /**
     * Version GỐC (BÌNH THƯỜNG, KHÔNG THỂ là future-plan khác — xem javadoc
     * class ở trên) làm baseline so sánh ROI kỳ vọng vs thực tế.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_version_id", nullable = false)
    private InvestmentProfileVersion sourceVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "strategy_id")
    private Strategy strategy;

    // ── Snapshot các tham số tài chính — clone từ sourceVersion lúc tạo (KHỚP
    // ĐÚNG kiểu dữ liệu với InvestmentProfileVersion để clone không cần ép kiểu) ──
    private Long equity;
    private Long loanCapital;
    private Long reserveFund;
    private Long expectedRoi;
    private String riskToleranceLevel;
    private Long durationYear;
    private LocalDate startDate;
    private String legalStatus;
    private String ward;
    private String conscious;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "investment_strategy_detail", columnDefinition = "json")
    private Map<String, Object> investmentStrategyDetail;

    /** Tổng hợp lợi nhuận đã tính (totalInvestedCapital, totalProfitPercentage, yieldDelta, aiComparisonNote...) — đọc lại khi GET, không tính lại. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "profit_summary", columnDefinition = "json")
    private Map<String, Object> profitSummary;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
