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

    public ListingDetailResponse toListingDetail(Listing l, Property p) {

        List<ListingImageResponse> images = toListingImageResponses(l);

        PropertyDetailResponse propertyDetail = p != null ? toPropertyDetail(p, null) : null;

        Seller seller = l.getSeller();
        Account sellerAccount = seller != null ? seller.getAccount() : null;

        ListingVerification lv = l.getListingVerification();

        return ListingDetailResponse.builder()
                .listingId(l.getListingId())
                .title(l.getTitle())
                .description(l.getDescription())
                .price(l.getPrice())
                .contactPerson(l.getContactPerson())
                .contactPersonName(l.getContactPersonName())
                .contactPersonPhone(l.getContactPersonPhone())
                .linkSocialContactPerson(l.getLinkSocialContactPerson())
                .viewingDate(l.getViewingDate())
                .startTime(l.getStartTime())
                .endTime(l.getEndTime())
                .isActive(l.getIsActive())
                .sellerStatus(l.getStatus() != null ? l.getStatus().name() : null)
                .verificationStatus(lv != null && lv.getStatus() != null ? lv.getStatus().name() : null)
                .reviewerNote(lv != null ? lv.getReviewerNote() : null)
                .property(propertyDetail)
                .images(images)
                .sellerId(seller != null ? seller.getSellerId() : null)
                .sellerName(sellerAccount != null ? sellerAccount.getFull_name() : null)
                .sellerAvatar(sellerAccount != null ? sellerAccount.getAvatar() : null)
                .sellerPhone(sellerAccount != null ? sellerAccount.getPhone() : null)
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
                .propertyAttribute(p.getPropertyAttribute())
                .propertyPurpose(p.getPropertyPurpose())
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

        String thumbnail = resolveThumbnailUrl(l);

        ListingVerification lv = l.getListingVerification();

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
                .sellerStatus(l.getStatus() != null ? l.getStatus().name() : null)
                .createdAt(l.getCreatedAt())
                .isFavorited(isFavorited)
                .verificationStatus(lv != null && lv.getStatus() != null ? lv.getStatus().name() : null)
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
