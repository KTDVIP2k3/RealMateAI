package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.PropertyValuationStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "property_valuation")
public class PropertyValuation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "property_valuation_id")
    private Integer propertyValuationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    private Long marketUnitPrice;
    private Long locationK;
    private Long gfa;
    private Long constructionNewPrice;
    private Long remainingQuantity;
    private Long landPrice;
     private Long constructionCost;
    private Long totalValue;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    private PropertyValuationStatusEnum propertyValuationStatus;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
