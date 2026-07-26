package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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
    private String contactPersonPhone;
    private String viewingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isActive;

    /** Trạng thái duyệt hiện tại: PENDING / APPROVED / REJECTED / EXPIRED */
    private String verificationStatus;

    /**
     * Badge "tích xanh" — true khi có yêu cầu xác thực giấy tờ pháp lý ĐÃ
     * ĐƯỢC Staff duyệt cho ĐÚNG Listing này (khác verificationStatus/status
     * duyệt nội dung ở trên). Không kế thừa khi tạo Listing mới từ cùng Property.
     */
    private Boolean isVerified;

    /** PENDING/APPROVED/REJECTED của yêu cầu tích xanh gần nhất — null nếu chưa từng yêu cầu. */
    private String certificationStatus;

    /** Lý do từ chối / ghi chú của Staff (nếu có) */
    private String reviewerNote;

    /** Thông số BĐS đầy đủ (property KHÔNG còn kèm ảnh — xem field "images" bên dưới) */
    private PropertyDetailResponse property;

    /**
     * Ảnh của CHÍNH bài đăng này (thay cho property.images cũ). Ảnh thumbnail
     * (isThumbnail = true) luôn được xếp lên đầu danh sách.
     */
    private List<ListingImageResponse> images;

    // Thông tin Seller cơ bản
    private Integer sellerId;
    private String sellerName;
    private String sellerPhone;

    private Integer viewCount;

    private String wardCode;

    private String email;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
