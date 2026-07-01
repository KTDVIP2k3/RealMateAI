package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ListingDetailResponse {

    private Integer listingId;
    private String title;
    private String description;
    private Long price;
    private String contactPerson;
    private String contactPersonName;
    private String contactPersonPhone;
    private String linkSocialContactPerson;
    private String viewingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isActive;

    /** Trạng thái duyệt hiện tại: PENDING / APPROVED / REJECTED / EXPIRED */
    private String verificationStatus;

    /** Lý do từ chối / ghi chú của Staff (nếu có) */
    private String reviewerNote;

    /** Thông số BĐS đầy đủ, kèm danh sách ảnh thực tế (property.images) */
    private PropertyDetailResponse property;

    // Thông tin Seller cơ bản
    private Integer sellerId;
    private String sellerName;
    private String sellerAvatar;
    private String sellerPhone;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
