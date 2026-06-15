package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "strategy")
public class Strategy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "strategy_id")
    private Integer strategyId;

    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<StrategyPortfolio> strategyPortfolios = new ArrayList<>();

    @OneToMany(mappedBy = "strategy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvestmentProfile> investmentProfiles = new ArrayList<>();
}
