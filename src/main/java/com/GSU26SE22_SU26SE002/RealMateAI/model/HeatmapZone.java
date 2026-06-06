package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.CrawPropertyListing;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "heatmap_zone")
public class HeatmapZone {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "heatmap_zone_id")
    private Integer heatmapZoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "craw_property_listing_id", nullable = false)
    private CrawPropertyListing crawPropertyListing;

    @Column(name = "zone_name")
    private String zoneName;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    private Double radius;

    private Long avgPricePerM2;

    private Integer transactionCount;

    private Double demandIndex;

    private String ward;
    private String district;
    private String city;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
