package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.PostingPackageOrderStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "posting_package_order")
public class PostingPackageOrder {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "posting_package_order_id")
    private Integer postingPackageOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posting_package_id", nullable = false)
    private PostingPackage postingPackage;


    private BigDecimal totalAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer duration;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PostingPackageOrderStatusEnum status;
}
