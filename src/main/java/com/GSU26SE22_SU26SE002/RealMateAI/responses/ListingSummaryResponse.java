package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ListingSummaryResponse {
    private Integer listingId;
    private String title;
    private Long price;

    // Thông số BĐS rút gọn
    private Double area;
    private Integer bedroom;
    private Integer bathroom;
    private String propertyTypeName;

    /** Ảnh đại diện (is_main=true), fallback ảnh đầu tiên */
    private String thumbnailUrl;

    private Boolean isActive;

    /** Toạ độ vị trí BĐS — lấy từ property.location (phẳng ra để FE cắm thẳng lên bản đồ, không cần đi sâu property.location) */
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;

    /** Số lượt xem — dùng để hiển thị cùng sort MOST_VIEWED */
    private Integer viewCount;

    private LocalDateTime createdAt;

    /** true nếu user hiện tại đã yêu thích bài đăng này */
    private Boolean isFavorited;

    /** Trạng thái duyệt hiện tại: PENDING / APPROVED / REJECTED / EXPIRED */
    private String verificationStatus;

    /** Badge "tích xanh" — xem giải thích đầy đủ ở ListingDetailResponse. */
    private Boolean isVerified;

    // MỚI: gói dịch vụ đăng tin ĐANG chạy cho tin này (PostingPackageOrder có
    // isActive=true) — null nếu tin chưa từng thanh toán thành công, hoặc gói
    // đã hết hạn (isActive tự về false khi hết hạn — xem cron/job hết hạn).
    private Integer currentPostingPackageId;
    private String currentPostingPackageName;
    private java.time.LocalDateTime currentPostingPackageEndDate;
}
