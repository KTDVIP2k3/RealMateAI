package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.*;
@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
@Entity @Table(name = "listing_image")
public class ListingImage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_image_id")
    private Integer listingImageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    /** true = ảnh thumbnail (đại diện), hiển thị đầu tiên. Chỉ 1 ảnh/listing. */
    @Column(name = "is_thumbnail")
    private Boolean isThumbnail;

    private Integer displayOrder;
}
