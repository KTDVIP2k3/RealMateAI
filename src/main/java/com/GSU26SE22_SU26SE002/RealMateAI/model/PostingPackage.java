package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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
@Entity @Table(name = "posting_package")
public class PostingPackage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "posting_package_id")
    private Integer postingPackageId;

    private String name;

    @Column(columnDefinition = "Text")
    private String description;
    private BigDecimal posting_package_price;
    private BigDecimal priority;
    /** Duration in days */
//    private Integer durationDays;
    private Boolean isActive;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "postingPackage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<PostingPackageOrder> postingPackageOrders = new ArrayList<>();
}
