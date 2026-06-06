package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackagePrice;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String description;
    /** Duration in days */
    private Integer durationDays;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "postingPackage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PostingPackagePrice> postingPackagePrices = new ArrayList<>();
}
