package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "image_listing")
public class ImageListing {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_listing_id")
    private Integer imageListingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    private String imageUrl;

    private Boolean isMain;
    private Integer displayOrder;
}
