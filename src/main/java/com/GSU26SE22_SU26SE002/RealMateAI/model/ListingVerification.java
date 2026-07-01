package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "listing_verification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "listing_verification_seq")
    @SequenceGenerator(
            name = "listing_verification_seq",
            sequenceName = "listing_verification_listing_verification_id_seq",
            allocationSize = 1
    )
    @Column(name = "listing_verification_id")
    private Integer listingVerificationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false, unique = true)
    private Listing listing;

    // Người duyệt cuối cùng — NULL khi record được tự tạo ở trạng thái PENDING
    // lúc Seller tạo/tạo lại Listing (chưa có Staff/Admin nào duyệt).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = true)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingStatusEnum status = ListingStatusEnum.PENDING;

    @Column(columnDefinition = "TEXT")
    private String reviewerNote;

    private LocalDateTime verifiedAt;

}