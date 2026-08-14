package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ListingMapper — Logic map Entity → Response DÙNG CHUNG giữa:
 *  - ListingServiceImplement              (POST/GET/PUT /listings)
 *  - ListingVerificationServiceImplement  (Staff/Admin duyệt tin)
 *
 * Tách thành @Component riêng để tránh trùng lặp code map Listing/Property/Image
 * giữa 2 service — đảm bảo mọi nơi hiển thị "chi tiết bài đăng" (kể cả màn hình
 * duyệt tin của Staff) đều thấy đúng 1 cấu trúc dữ liệu, không bị lệch nhau.
 *
 * LƯU Ý (sau khi tách ảnh khỏi Property): ảnh nay thuộc về Listing
 * (Listing#listingImages), Property CHỈ còn thông số bất động sản thuần tuý.
 * Ảnh thumbnail (isThumbnail = true) LUÔN được xếp lên đầu danh sách "images",
 * sau đó mới theo displayOrder.
 */


@Component
public class ListingMapper {

    /** Sắp xếp: ảnh thumbnail lên đầu, còn lại theo displayOrder tăng dần. */
    private static final Comparator<ListingImage> THUMBNAIL_FIRST_ORDER =
            Comparator.comparing((ListingImage img) -> !Boolean.TRUE.equals(img.getIsThumbnail()))
                    .thenComparing(ListingImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * Chi tiết đầy đủ 1 tin đăng — dùng CHUNG cho MỌI nơi hiển thị (public
     * GET /listings/{id}, GET /seller/listings/{id}, Staff duyệt tin, response
     * của POST /listings lúc tạo...). KHÔNG còn phân biệt 2 bản "public" vs
     * "owner" nữa — sellerAvatar/sellerStatus/contactPersonName/
     * linkSocialContactPerson đã bỏ HẲN khỏi mọi nơi (không chỉ null ở 1 view
     * cụ thể), wardCode (mã vùng) + email luôn được trả kèm ở MỌI nơi.
     */
    public ListingDetailResponse toListingDetail(Listing l, Property p) {

        List<ListingImageResponse> images = toListingImageResponses(l);

        PropertyDetailResponse propertyDetail = p != null ? toPropertyDetail(p, null) : null;

        Seller seller = l.getSeller();
        Account sellerAccount = seller != null ? seller.getAccount() : null;
        Ward ward = (p != null && p.getLocation() != null) ? p.getLocation().getWard() : null;

        ListingVerification lv = l.getListingVerification();

        return ListingDetailResponse.builder()
                .listingId(l.getListingId())
                .title(l.getTitle())
                .description(l.getDescription())
                .price(l.getPrice())
                .contactPerson(l.getContactPerson())
                .contactPersonPhone(l.getContactPersonPhone())
                .isActive(l.getIsActive())
                .verificationStatus(lv != null && lv.getStatus() != null ? lv.getStatus().name() : null)
                .reviewerNote(lv != null ? lv.getReviewerNote() : null)
                .isVerified(l.getIsVerified())
                .certificationStatus(l.getCertificationStatus() != null ? l.getCertificationStatus().name() : null)
                .property(propertyDetail)
                .images(images)
                .sellerId(seller != null ? seller.getSellerId() : null)
                .sellerName(sellerAccount != null ? sellerAccount.getFull_name() : null)
                .sellerPhone(sellerAccount != null ? sellerAccount.getPhone() : null)
                .viewCount(l.getViewCount())
                .wardCode(ward != null ? ward.getWard_code() : null)
                .email(l.getContactEmail() != null ? l.getContactEmail()
                        : (sellerAccount != null ? sellerAccount.getEmail() : null))
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    /**
     * @param activeListingCount số Listing đang tham chiếu tới Property (có thể null nếu
     *                           không cần hiển thị, ví dụ khi map lồng trong ListingDetailResponse)
     */
    public PropertyDetailResponse toPropertyDetail(Property p, Integer activeListingCount) {
        Location loc = p.getLocation();
        Ward ward = loc != null ? loc.getWard() : null;

        return PropertyDetailResponse.builder()
                .propertyId(p.getPropertyId())
                .title(p.getTitle())
                .description(p.getDescription())
                .area(p.getArea())
                .price(p.getPrice())
                .floor(p.getFloor())
                .bedroom(p.getBedroom())
                .bathroom(p.getBathroom())
                .direction(p.getDirection())
                .legalStatus(p.getLegalStatus())
                .addressParticular(p.getAddressParticular())
                .projectName(p.getProjectName())
                .furniture(p.getFurniture())
                .propertyTypeName(p.getPropertyType() != null ? p.getPropertyType().getName() : null)
                .propertyConditionName(p.getPropertyCondition() != null ? p.getPropertyCondition().getName() : null)
                .latitude(loc != null ? loc.getLatitude() : null)
                .longitude(loc != null ? loc.getLongitude() : null)
                .postalCode(loc != null ? loc.getPostalCode() : null)
                .wardCode(ward != null ? ward.getWard_code() : null)
                .wardName(ward != null ? ward.getName() : null)
                .isActive(p.getIsActive())
                .activeListingCount(activeListingCount)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public ListingSummaryResponse toListingSummary(Listing l, boolean isFavorited) {
        Property p = l.getProperty();
        Location loc = p != null ? p.getLocation() : null;

        String thumbnail = resolveThumbnailUrl(l);

        ListingVerification lv = l.getListingVerification();

        // MỚI: tìm PostingPackageOrder ĐANG active (isActive=true) — về nghiệp
        // vụ tại 1 thời điểm chỉ có tối đa 1 order thật sự "đang chạy" cho 1
        // Listing, nhưng vẫn duyệt an toàn qua toàn bộ danh sách + lấy
        // endDate xa nhất phòng trường hợp dữ liệu có nhiều dòng active hơn
        // dự kiến (không throw lỗi, chỉ lấy 1 đại diện hợp lý nhất).
        PostingPackageOrder currentOrder = l.getPostingPackageOrders() == null ? null
                : l.getPostingPackageOrders().stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsActive()))
                .max(Comparator.comparing(PostingPackageOrder::getEndDate, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);

        return ListingSummaryResponse.builder()
                .listingId(l.getListingId())
                .title(l.getTitle())
                .price(l.getPrice())
                .area(p != null ? p.getArea() : null)
                .bedroom(p != null ? p.getBedroom() : null)
                .bathroom(p != null ? p.getBathroom() : null)
                .propertyTypeName(p != null && p.getPropertyType() != null ? p.getPropertyType().getName() : null)
                .thumbnailUrl(thumbnail)
                .isActive(l.getIsActive())
                .latitude(loc != null ? loc.getLatitude() : null)
                .longitude(loc != null ? loc.getLongitude() : null)
                .viewCount(l.getViewCount())
                .createdAt(l.getCreatedAt())
                .isFavorited(isFavorited)
                .verificationStatus(lv != null && lv.getStatus() != null ? lv.getStatus().name() : null)
                .isVerified(l.getIsVerified())
                .currentPostingPackageId(currentOrder != null && currentOrder.getPostingPackage() != null
                        ? currentOrder.getPostingPackage().getPostingPackageId() : null)
                .currentPostingPackageName(currentOrder != null && currentOrder.getPostingPackage() != null
                        ? currentOrder.getPostingPackage().getName() : null)
                .currentPostingPackageEndDate(currentOrder != null ? currentOrder.getEndDate() : null)
                .build();
    }

    /** Ảnh đại diện: ưu tiên isThumbnail=true, fallback ảnh có displayOrder nhỏ nhất. */
    public String resolveThumbnailUrl(Listing l) {
        List<ListingImage> imgs = l.getListingImages();
        if (imgs == null || imgs.isEmpty()) return null;
        return imgs.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsThumbnail()))
                .map(ListingImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> imgs.stream()
                        .min(Comparator.comparing(ListingImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(ListingImage::getImageUrl)
                        .orElse(null));
    }

    public List<ListingImageResponse> toListingImageResponses(Listing l) {
        if (l.getListingImages() == null || l.getListingImages().isEmpty()) {
            return Collections.emptyList();
        }
        return l.getListingImages().stream()
                .sorted(THUMBNAIL_FIRST_ORDER)
                .map(this::toListingImageResponse)
                .collect(Collectors.toList());
    }

    public ListingImageResponse toListingImageResponse(ListingImage img) {
        return ListingImageResponse.builder()
                .listingImageId(img.getListingImageId())
                .imageUrl(img.getImageUrl())
                .isThumbnail(img.getIsThumbnail())
                .displayOrder(img.getDisplayOrder())
                .build();
    }
}
