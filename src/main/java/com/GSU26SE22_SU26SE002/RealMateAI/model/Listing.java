package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.CertificationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.SellerListingStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
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

    @Column(name = "contact_person")
    private String contactPerson;
    @Column(name = "contact_person_name")
    private String contactPersonName;
    @Column(name = "contact_person_phone")
    private String contactPersonPhone;
    /** Email liên hệ RIÊNG cho tin đăng này — nếu để trống, fallback dùng email của Account Seller (xem ListingMapper#toListingDetail). */
    @Column(name = "contact_email")
    private String contactEmail;
    @Column(name = "link_social_contact_person")
    private String linkSocialContactPerson;
    @Column(name = "viewing_date")
    private String viewingDate;
    @Column(name = "start_time")
    private LocalTime startTime;
    @Column(name = "end_time")
    private LocalTime endTime;
    private Boolean isActive;


    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    /**
     * MỚI: mức độ ưu tiên hiển thị — lấy từ PostingPackage.priority của gói
     * dịch vụ đang ACTIVE cho tin này (1-4, số càng lớn càng ưu tiên hiển thị
     * lên đầu danh sách). Cập nhật tại thời điểm thanh toán/thanh toán lại
     * THÀNH CÔNG (xem PostingPackageOrderServiceImplement#transitionListingForNewPackageOrder),
     * KHÔNG tính realtime bằng subquery (đơn giản hoá, đánh đổi: nếu gói hết
     * hạn mà không có job reset lại — GIỚI HẠN hiện tại, cần bổ sung cron
     * riêng để reset priority=0 khi PostingPackageOrder hết hạn).
     */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private SellerListingStatusEnum status = SellerListingStatusEnum.ACTIVE;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Badge "tích xanh" — chỉ true khi CÓ 1 ListingCertificationRequest được
     * Staff APPROVED cho ĐÚNG Listing này. Mặc định false cho MỌI Listing
     * mới tạo, KỂ CẢ khi tạo từ 1 Property đã có Listing khác từng được tích
     * xanh trước đó — tích xanh KHÔNG kế thừa qua Property, phải yêu cầu lại
     * cho từng Listing (đúng yêu cầu nghiệp vụ).
     */
    @Builder.Default
    @Column(name = "is_verified")
    private Boolean isVerified = false;

    /**
     * Trạng thái yêu cầu tích xanh GẦN NHẤT — null nghĩa là CHƯA từng yêu cầu.
     * Lưu thêm ở đây (thay vì luôn phải JOIN certificationRequests) để FE
     * hiển thị nhanh "chưa yêu cầu / đang chờ duyệt / đã tích xanh / bị từ
     * chối" mà không cần load toàn bộ lịch sử yêu cầu.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "certification_status", length = 20)
    private CertificationStatusEnum certificationStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FavoriteListing> favoriteListings = new ArrayList<>();

    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ListingVerification listingVerification;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PostingPackageOrder> postingPackageOrders = new ArrayList<>();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ListingCertificationRequest> certificationRequests = new ArrayList<>();

    @BatchSize(size = 30)
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ListingImage> listingImages = new ArrayList<>();
}