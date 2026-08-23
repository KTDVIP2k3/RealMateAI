package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.InvestmentCriteria;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposed_property")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProposedProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proposed_property_id")
    private Integer proposedPropertyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criteria_id", nullable = false)
    private InvestmentCriteria investmentCriteria;

    @Column(name = "listing_id", nullable = false)
    private Integer listingId;

    @Column(name = "proposal_type", nullable = false)
    private String proposalType;

    @Column(name = "property_project_name")
    private String propertyProjectName;

    @Column(name = "area")
    private Integer area;

    @Column(name = "value_price")
    private Double valuePrice;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "estimated_profit")
    private Long estimatedProfit;

    @Column(name = "estimated_price_growth")
    private Long estimatedPriceGrowth;

    @Column(name = "monthly_rental_cashflow")
    private Long monthlyRentalCashflow;

    @Column(name = "monthly_principal_interest")
    private Long monthlyPrincipalInterest;

    @Column(name = "net_cashflow")
    private Long netCashflow;

    @Column(name = "roi_percentage")
    private Double roiPercentage;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}