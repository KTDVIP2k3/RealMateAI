package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateListingRequest {

    // ── Listing ───────────────────────────────────────────
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

    // ── Property ──────────────────────────────────────────
    private String propertyTitle;
    private String propertyDescription;
    private Long propertyPrice;
    private Double area;
    private Integer floor;
    private Integer bedroom;
    private Integer bathroom;
    private String direction;
    private Integer propertyTypeId;
    private Integer propertyConditionId;

    // ── Location ──────────────────────────────────────────
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String postalCode;
    private String wardCode;

    // ── Ảnh bổ sung (tuỳ chọn) ──────────────────────────────
    /**
     * Nếu Seller muốn bổ sung thêm ảnh khi sửa bài đăng, upload trước qua
     * POST /api/v1/media/upload/multiple?entityType=ACCOUNT&entityId={accountId}
     * rồi gửi publicId vào đây — ảnh sẽ được NỐI THÊM vào bộ ảnh hiện có
     * của Property (không xóa ảnh cũ). Có thể để trống nếu không đổi ảnh.
     */
    private List<String> draftImagePublicIds;

    private Integer mainImageIndex;
}
