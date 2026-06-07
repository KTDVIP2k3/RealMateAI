package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "portfolio_allocation_property")
public class PortfolioAllocationProperty {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "portfolio_allocation_property_id")
    private Integer portfolioAllocationPropertyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_allocation_id", nullable = false)
    private PortfolioAllocation portfolioAllocation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    private Double weight;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
