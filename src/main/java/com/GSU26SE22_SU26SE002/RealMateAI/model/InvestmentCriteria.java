package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter
@Entity @Table(name = "investment_criteria")
public class InvestmentCriteria {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investment_criteria_id")
    private Integer investmentCriteriaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_version_id", nullable = false)
    @JsonIgnore
    private InvestmentProfileVersion investmentProfileVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_type_id")
    @JsonIgnore
    private PropertyType propertyType;

    @OneToMany( mappedBy = "investmentCriteria", cascade = CascadeType.ALL,fetch =  FetchType.LAZY)
    private List<ProposedProperty> proposedProperties;


//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "property_condition_id")
//    @JsonIgnore
//    private PropertyCondition propertyCondition;

////    private Long minPrice;
////    private Long maxPrice;
////    private Double minArea;
////    private Double maxArea;
////    private String location;
////    private Boolean isActive;
//    private LocalDateTime createdAt;
//    private LocalDateTime updatedAt;
}
