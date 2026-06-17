package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.FavoriteListing;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "listing")
public class Listing {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "listing_id")
    private Integer listingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Long price;

    @Column(name = "contact_person")            private String contactPerson;
    @Column(name = "contact_person_name")       private String contactPersonName;
    @Column(name = "contact_person_phone")      private String contactPersonPhone;
    @Column(name = "link_social_contact_person")private String linkSocialContactPerson;
    @Column(name = "viewing_date")              private String viewingDate;
    @Column(name = "start_time")                private LocalTime startTime;
    @Column(name = "end_time")                  private LocalTime endTime;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FavoriteListing> favoriteListings = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ListingVerification> listingVerifications = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PostingPackageOrder> postingPackageOrders = new ArrayList<>();
}
