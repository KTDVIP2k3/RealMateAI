package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.HeatmapZone;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
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

    @Column(name = "source_url", unique = true, length = 500, nullable = false)
    private String sourceUrl;

    @Column(name = "price", precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "area", precision = 18, scale = 2)
    private BigDecimal area;

    @Column(name = "price_per_m2", precision = 18, scale = 2)
    private BigDecimal pricePerM2;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private Date posted_date;

    private Timestamp craw_date;


}
