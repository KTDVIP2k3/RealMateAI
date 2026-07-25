package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Table(name = "posting_package_category")
public class PostingPackageCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "posting_package_category_id")
    private Integer postingPackageCategoryId;

    @Column(name = "posting_package_category_name")
    private String postingPackageCategoryName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal priority;

    private Boolean isActive;
    private Boolean isDeleted;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "postingPackageCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("postingPackageCategory")
    @Builder.Default
    private List<PostingPackage> postingPackageList = new ArrayList<>();
}