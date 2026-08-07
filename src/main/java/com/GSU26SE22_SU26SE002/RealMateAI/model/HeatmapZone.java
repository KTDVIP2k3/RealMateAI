package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "heatmap_zone")
public class HeatmapZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "zoom_level", nullable = false)
    private Integer zoomLevel;

    @Column(name = "grid_x", nullable = false)
    private Integer gridX;

    @Column(name = "grid_y", nullable = false)
    private Integer gridY;

    @Column(name = "center_latitude", nullable = false)
    private Double centerLatitude;

    @Column(name = "center_longitude", nullable = false)
    private Double centerLongitude;

    @Column(name = "listing_count")
    private Integer listingCount;

    @Column(name = "median_price_per_m2", precision = 18, scale = 2)
    private BigDecimal medianPricePerM2;

    @Column(name = "density_heat_level")
    private Integer densityHeatLevel;

    @Column(name = "price_heat_level", precision = 5, scale = 2)
    private BigDecimal priceHeatLevel;

    @ManyToMany(mappedBy = "heatmapZones", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<CrawPropertyListing> listings = new ArrayList<>();
}