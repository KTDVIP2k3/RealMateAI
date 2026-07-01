package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ListingDetailResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ListingSummaryResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PropertyDetailResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PropertyImageResponse;
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
 */
@Component
public class ListingMapper {
    public ListingDetailResponse toListingDetail(Listing l, Property p) {

        List<PropertyImageResponse> propertyImages = (p == null || p.getPropertyImages() == null)
                ? Collections.emptyList()
                : p.getPropertyImages().stream()
                .sorted(Comparator.comparing(PropertyImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toPropertyImageResponse)
                .collect(Collectors.toList());

        PropertyDetailResponse propertyDetail = null;
        if (p != null) {
            propertyDetail = toPropertyDetail(p, propertyImages, null);
        }

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
                .verificationStatus(lv != null && lv.getStatus() != null ? lv.getStatus().name() : null)
                .reviewerNote(lv != null ? lv.getReviewerNote() : null)
                .property(propertyDetail)
                .sellerId(seller != null ? seller.getSellerId() : null)
                .sellerName(sellerAccount != null ? sellerAccount.getFull_name() : null)
                .sellerAvatar(sellerAccount != null ? sellerAccount.getAvatar() : null)
                .sellerPhone(sellerAccount != null ? sellerAccount.getPhone() : null)
                .createdAt(l.getCreatedAt())
                .updatedAt(l.getUpdatedAt())
                .build();
    }

    public PropertyDetailResponse toPropertyDetail(Property p, List<PropertyImageResponse> images, Integer activeListingCount) {
        Location loc = p.getLocation();
        Ward ward = loc != null ? loc.getWard() : null;

        List<PropertyImageResponse> resolvedImages = images != null ? images
                : (p.getPropertyImages() == null ? Collections.emptyList()
                : p.getPropertyImages().stream()
                .sorted(Comparator.comparing(PropertyImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toPropertyImageResponse)
                .collect(Collectors.toList()));

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
                .images(resolvedImages)
                .isActive(p.getIsActive())
                .activeListingCount(activeListingCount)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    public ListingSummaryResponse toListingSummary(Listing l, boolean isFavorited) {
        Property p = l.getProperty();

        String thumbnail = (p == null || p.getPropertyImages() == null || p.getPropertyImages().isEmpty())
                ? null
                : p.getPropertyImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsMain()))
                .map(PropertyImage::getImageUrl)
                .findFirst()
                .orElseGet(() -> p.getPropertyImages().stream()
                        .min(Comparator.comparing(PropertyImage::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(PropertyImage::getImageUrl)
                        .orElse(null));

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
                .createdAt(l.getCreatedAt())
                .isFavorited(isFavorited)
                .verificationStatus(lv != null && lv.getStatus() != null ? lv.getStatus().name() : null)
                .build();
    }

    public PropertyImageResponse toPropertyImageResponse(PropertyImage img) {
        return PropertyImageResponse.builder()
                .propertyImageId(img.getPropertyImageId())
                .imageUrl(img.getImageUrl())
                .isMain(img.getIsMain())
                .displayOrder(img.getDisplayOrder())
                .build();
    }
}
