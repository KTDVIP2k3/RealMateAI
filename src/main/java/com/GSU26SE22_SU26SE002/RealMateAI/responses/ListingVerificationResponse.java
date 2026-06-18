package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingVerificationResponse {
    private Integer listingVerificationId;
    private ListingStatusEnum status;
    private String reviewerNote;
    private LocalDateTime verifiedAt;

    private Integer reviewerAccountId;
    private String reviewerName;

    /**
     * Snapshot bài đăng kèm CẢ NỘI DUNG VÀ ẢNH tại thời điểm duyệt —
     * để Staff/Admin xem xét đầy đủ trong 1 màn hình, không cần gọi
     * API riêng để lấy ảnh.
     */
    private ListingDetailResponse listing;
}
