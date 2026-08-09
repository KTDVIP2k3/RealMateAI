package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * FutureInvestmentScenario — mirror của {@link InvestmentScenario} nhưng
 * link tới {@link FutureInvestmentPlan} (KHÔNG còn tới InvestmentProfileVersion).
 *
 * FIX so với InvestmentScenario cũ: InvestmentScenario CHỈ có 3 field hữu ích
 * (scenarioType, expectedReturnRate, description) trong khi AI (Gemini) trả
 * về NHIỀU field hơn (monthlyCashflow, probability, durationMonths,
 * priceGrowthMin/Max) — các field này bị ÂM THẦM MẤT vì entity cũ không có
 * cột lưu, dẫn tới GET lại toàn null. Entity mới lưu ĐỦ toàn bộ field AI trả về.
 */
@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
@Entity @Table(name = "future_investment_scenario")
public class FutureInvestmentScenario {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "future_investment_scenario_id")
    private Integer futureInvestmentScenarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "future_investment_plan_id", nullable = false)
    @JsonIgnore
    private FutureInvestmentPlan futureInvestmentPlan;

    private String name;
    private String scenarioType;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** % lợi nhuận kỳ vọng của kịch bản (vd 0.2412 = 24.12%) */
    private Double expectedReturnRate;

    /** Dòng tiền hàng tháng dự kiến trong kịch bản này (VNĐ) */
    private Double monthlyCashflow;

    /** Xác suất xảy ra kịch bản (0.0 -> 1.0) */
    private Double probability;

    /** Thời gian kịch bản này dự kiến kéo dài (tháng) */
    private Integer durationMonths;

    /** Khoảng tăng trưởng giá tối thiểu/tối đa dự kiến trong kịch bản (%) */
    private Double priceGrowthMin;
    private Double priceGrowthMax;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
