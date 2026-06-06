package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackage;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackageOrder;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Data @Builder
@Entity @Table(name = "posting_package_price")
public class PostingPackagePrice {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "posting_package_price_id")
    private Integer postingPackagePriceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posting_package_id", nullable = false)
    private PostingPackage postingPackage;

    private BigDecimal price;
    private LocalDateTime effectiveDate;
    private LocalDateTime expiryDate;
    private Boolean isActive;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "postingPackagePrice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PostingPackageOrder> postingPackageOrders = new ArrayList<>();
}
