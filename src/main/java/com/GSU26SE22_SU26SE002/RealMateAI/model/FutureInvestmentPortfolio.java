package com.GSU26SE22_SU26SE002.RealMateAI.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FutureInvestmentPortfolio — mirror của {@link InvestmentPortfolio}, link
 * tới {@link FutureInvestmentPlan} thay vì InvestmentProfileVersion.
 *
 * ĐƠN GIẢN HOÁ so với luồng "version bình thường": bỏ bảng trung gian
 * PortfolioAllocation (ở luồng cũ mỗi InvestmentPortfolio chỉ có đúng 1
 * PortfolioAllocation — bảng trung gian không mang thêm ý nghĩa gì ở nhánh
 * Future Plan) — properties được chọn gắn THẲNG vào đây qua
 * {@link #futurePortfolioAllocationProperties}.
 */
@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
@Entity @Table(name = "future_investment_portfolio")
public class FutureInvestmentPortfolio {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "future_investment_portfolio_id")
    private Integer futureInvestmentPortfolioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "future_investment_plan_id", nullable = false)
    private FutureInvestmentPlan futureInvestmentPlan;

    /** Danh mục (Tăng trưởng / Thanh khoản...) — bảng Portfolio dùng chung với luồng bình thường. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    private Integer percentage;
    private Long capital;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "futureInvestmentPortfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FuturePortfolioAllocationProperty> futurePortfolioAllocationProperties = new ArrayList<>();
}
