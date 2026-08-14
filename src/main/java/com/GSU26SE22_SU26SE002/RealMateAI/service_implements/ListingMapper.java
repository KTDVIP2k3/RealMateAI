package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.PostingPackageOrderStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class ListingMapper {

    private static final Comparator<ListingImage> THUMBNAIL_FIRST_ORDER =
            Comparator.comparing((ListingImage img) -> !Boolean.TRUE.equals(img.getIsThumbnail()))
                    .thenComparing(ListingImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()));

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
                .viewCount(l.getViewCount() != null ? l.getViewCount() : 0)
                .wardCode(ward != null ? ward.getWard_code() : null)
                .email(l.getContactEmail() != null ? l.getContactEmail()
                        : (sellerAccount != null ? sellerAccount.getEmail() : null))
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

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

    /** Overload cũ — không truyền realViewCount. */
    public ListingSummaryResponse toListingSummary(Listing l, boolean isFavorited) {
        return toListingSummary(l, isFavorited, null);
    }

    public ListingSummaryResponse toListingSummary(Listing l, boolean isFavorited, Long realViewCount) {
        Property p = l.getProperty();
        Location loc = p != null ? p.getLocation() : null;
        String thumbnail = resolveThumbnailUrl(l);
        ListingVerification lv = l.getListingVerification();

        PostingPackageOrder currentOrder = resolveCurrentOrder(l);
        PostingPackage currentPackage = currentOrder != null ? currentOrder.getPostingPackage() : null;

        String categoryName = null;
        if (currentPackage != null && currentPackage.getPostingPackageCategory() != null) {
            categoryName = currentPackage.getPostingPackageCategory().getPostingPackageCategoryName();
        }

        int safeViewCount = realViewCount != null
                ? realViewCount.intValue()
                : (l.getViewCount() != null ? l.getViewCount() : 0);

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
                .viewCount(safeViewCount)
                .createdAt(l.getCreatedAt())
                .isFavorited(isFavorited)
                .verificationStatus(lv != null && lv.getStatus() != null ? lv.getStatus().name() : null)
                .isVerified(l.getIsVerified())
                .postingPackageCategoryName(categoryName)
                .build();
    }

    /**
     * Ưu tiên: isActive=true (endDate xa nhất)
     * → status SUCCESS (createdAt mới nhất)
     * → order bất kỳ (createdAt mới nhất)
     */
    private PostingPackageOrder resolveCurrentOrder(Listing l) {
        if (l.getPostingPackageOrders() == null || l.getPostingPackageOrders().isEmpty()) {
            return null;
        }
        return l.getPostingPackageOrders().stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsActive()))
                .max(Comparator.comparing(PostingPackageOrder::getEndDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .or(() -> l.getPostingPackageOrders().stream()
                        .filter(o -> o.getStatus() == PostingPackageOrderStatusEnum.SUCCESS)
                        .max(Comparator.comparing(PostingPackageOrder::getCreatedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))))
                .or(() -> l.getPostingPackageOrders().stream()
                        .max(Comparator.comparing(PostingPackageOrder::getCreatedAt,
                                Comparator.nullsFirst(Comparator.naturalOrder()))))
                .orElse(null);
    }

    public String resolveThumbnailUrl(Listing l) {
        List<ListingImage> imgs = l.getListingImages();
        if (imgs == null || imgs.isEmpty()) return null;
        return imgs.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsThumbnail()))
                .map(ListingImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> imgs.stream()
                        .min(Comparator.comparing(ListingImage::getDisplayOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
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