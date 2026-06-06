package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.InvestmentPortfolio;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Portfolio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "portfolio_allocation")
public class PortfolioAllocation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_allocation_id")
    private Integer portfolioAllocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    /** M:N với InvestmentPortfolio (theo ERD có ERmany-ERmany) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_portfolio_id")
    private InvestmentPortfolio investmentPortfolio;

    private Double allocationPercentage;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "portfolioAllocation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PortfolioAllocationProperty> portfolioAllocationProperties = new ArrayList<>();
}
