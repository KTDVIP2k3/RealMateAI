package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "zoom_level", nullable = false)
    private Integer zoomLevel;

    @Column(name = "grid_x", nullable = false)
    private Integer gridX;

    @Column(name = "grid_y", nullable = false)
    private Integer gridY;

    @Column(name = "center_latitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal centerLatitude;

    @Column(name = "center_longitude", precision = 10, scale = 7, nullable = false)
    private BigDecimal centerLongitude;

    @Column(name = "listing_count")
    private Integer listingCount;

    @Column(name = "median_price_per_m2", precision = 18, scale = 2)
    private BigDecimal medianPricePerM2;

    @Column(name = "price_change_percent", precision = 5, scale = 2)
    private BigDecimal priceChangePercent;
}