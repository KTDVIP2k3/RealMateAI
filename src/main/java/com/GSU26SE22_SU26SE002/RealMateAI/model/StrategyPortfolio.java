//package com.GSU26SE22_SU26SE002.RealMateAI.model;
//
//import com.GSU26SE22_SU26SE002.RealMateAI.model.Portfolio;
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.time.LocalDateTime;
//
//@NoArgsConstructor @AllArgsConstructor @Getter
//@Setter  @Builder
//@Entity @Table(name = "strategy_portfolio")
//public class StrategyPortfolio {
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "strategy_portfolio_id")
//    private Integer strategyPortfolioId;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JsonIgnore
//    @JoinColumn(name = "strategy_id", nullable = false)
//    private Strategy strategy;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JsonIgnore
//    @JoinColumn(name = "portfolio_id", nullable = false)
//    private Portfolio portfolio;
//
//    private Boolean isActive;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
//}
