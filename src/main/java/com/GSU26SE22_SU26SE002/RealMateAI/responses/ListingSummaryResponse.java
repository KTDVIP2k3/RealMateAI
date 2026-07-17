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

    /** Trạng thái Seller tự quản lý: ACTIVE / HIDDEN / DELETED */
    private String sellerStatus;

    private LocalDateTime createdAt;

    /** true nếu user hiện tại đã yêu thích bài đăng này */
    private Boolean isFavorited;

    /** Trạng thái duyệt hiện tại: PENDING / APPROVED / REJECTED / EXPIRED */
    private String verificationStatus;
}
