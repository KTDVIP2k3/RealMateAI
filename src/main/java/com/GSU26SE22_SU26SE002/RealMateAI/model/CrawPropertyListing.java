package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.HeatmapZone;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter // @Builder
@Entity
@Table(name = "craw_property_listing")
public class CrawPropertyListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "craw_property_listing_id")
    private Integer crawPropertyListingId;

    private String sourceUrl;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String rawData;

    private Long price;
    private Double area;
    private String address;
    private String propertyTypeName;
    private String status;
    private LocalDateTime crawledAt;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "crawPropertyListing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<HeatmapZone> heatmapZones = new ArrayList<>();
}
