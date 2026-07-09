package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.CrawPropertyListing;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter @Builder
@Entity
@Table(name = "heatmap_zone")
public class HeatmapZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "heatmap_zone_id")
    private Integer heatmapZoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "craw_property_listing_id", nullable = false)
    private CrawPropertyListing crawPropertyListing;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "location_id")
//    private Location location;

    @Column(name = "hot_score")
    private Integer hotScore;

    @Column(name = "price_average", precision = 18, scale = 2)
    private BigDecimal priceAverage;

    @Column(name = "total_listing")
    private Integer totalListing;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
