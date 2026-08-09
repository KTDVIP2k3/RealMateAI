package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.CertificationStatusEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "listing_certification_request")
public class ListingCertificationRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certification_request_id")
    private Integer certificationRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    @JsonIgnore
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    @JsonIgnore
    private Seller seller;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CertificationStatusEnum status;

    @Column(name = "reviewer_note", columnDefinition = "TEXT")
    private String reviewerNote;

    /** Staff/Admin đã duyệt — null khi còn PENDING. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    @JsonIgnore
    private Account reviewedBy;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    /**
     * Giấy tờ nộp kèm yêu cầu này — xem PropertyImage#certificationRequest.
     * KHÔNG dùng JOIN FETCH cho collection này ở repository (đã học từ sự cố
     * "could not identify an equality operator for type json" khi JOIN FETCH
     * 1-N + DISTINCT trước đây) — dùng @BatchSize để Hibernate tự gom nhiều
     * request cần load documents trong cùng transaction thành 1 câu
     * "SELECT ... WHERE certification_request_id IN (...)" duy nhất.
     */
    @BatchSize(size = 20)
    @OneToMany(mappedBy = "certificationRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PropertyImage> documents = new ArrayList<>();
}
