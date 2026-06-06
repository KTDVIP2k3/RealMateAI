package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "investment_criteria")
public class InvestmentCriteria {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investment_criteria_id")
    private Integer investmentCriteriaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_profile_id", nullable = false)
    private InvestmentProfile investmentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_type_id")
    private PropertyType propertyType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_condition_id")
    private PropertyCondition propertyCondition;

    private Long minPrice;
    private Long maxPrice;
    private Double minArea;
    private Double maxArea;
    private String location;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
