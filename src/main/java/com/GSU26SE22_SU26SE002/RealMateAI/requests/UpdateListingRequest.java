package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "Sửa nội dung tin đăng + thông số BĐS. Mọi field optional (null = giữ nguyên).")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateListingRequest {

    // ── Listing ───────────────────────────────────────────
    @Schema(example = "Căn hộ 2PN view sông - Quận 7 (đã giảm giá)")
    private String title;

    @Schema(example = "Nội thất đầy đủ, tầng 15, view sông Sài Gòn thoáng mát")
    private String description;

    @Schema(example = "3300000000")
    private Long price;

    @Schema(example = "owner")
    private String contactPerson;

    @Schema(example = "0901234567")
    private String contactPersonPhone;

    @Schema(description = "Email liên hệ RIÊNG cho tin đăng này (optional). null = giữ nguyên, không đổi.", example = "seller-contact@gmail.com")
    private String contactEmail;

    @Schema(example = "2025-08-01")
    private String viewingDate;

    @Schema(example = "09:00:00", type = "string")
    private LocalTime startTime;

    @Schema(example = "17:00:00", type = "string")
    private LocalTime endTime;

    // ── Property ──────────────────────────────────────────
    @Schema(example = "Căn hộ Gateway Thảo Điền")
    private String propertyTitle;

    @Schema(example = "Căn hộ cao cấp, nội thất nhập khẩu")
    private String propertyDescription;

    @Schema(example = "4000000000")
    private Long propertyPrice;

    @Schema(example = "75.5")
    private Double area;

    @Schema(example = "15")
    private Integer floor;

    @Schema(example = "2")
    private Integer bedroom;

    @Schema(example = "2")
    private Integer bathroom;

    @Schema(example = "Đông Nam")
    private String direction;

    @Schema(description = "Nội thất — mô tả tự do", example = "Đầy đủ nội thất")
    private String furniture;

    @Schema(example = "1")
    private Integer propertyTypeId;

    @Schema(example = "1")
    private Integer propertyConditionId;

    // ── Location ──────────────────────────────────────────
    @Schema(example = "10.8039")
    private BigDecimal latitude;

    @Schema(example = "106.7346")
    private BigDecimal longitude;

    @Schema(example = "700000")
    private String postalCode;

    @Schema(example = "26734")
    private String wardCode;

    // ── Ảnh (2 field TÁCH BIỆT — xem javadoc class ở trên) ──

    @Schema(
            description = "OPTIONAL. Publicid ảnh MỚI đã upload trước qua POST "
                    + "/media/upload/multiple?entityType=ACCOUNT&entityId={accountId}. "
                    + "Ảnh mới sẽ NỐI THÊM vào bộ ảnh hiện có (không xoá ảnh cũ). "
                    + "Để trống nếu không thêm ảnh.",
            example = "[\"realmateai/listings/ghi789\"]"
    )
    private List<String> draftImagePublicIds;

    @Schema(
            description = "OPTIONAL. Vị trí (0-based) trong \"draftImagePublicIds\" ở trên "
                    + "xác định ảnh MỚI nào làm thumbnail. CHỈ có ý nghĩa khi "
                    + "\"draftImagePublicIds\" không rỗng. Nếu truyền field này, thumbnail "
                    + "hiện tại (nếu có) sẽ được thay thế bằng ảnh mới này.",
            example = "0"
    )
    private Integer thumbnailImageIndex;

    @Schema(
            description = "OPTIONAL. Đổi thumbnail sang 1 ảnh ĐÃ CÓ SẴN của chính Listing "
                    + "này — truyền listingImageId lấy từ field \"images[].listingImageId\" "
                    + "trong response GET /seller/listings/{listingId}. KHÔNG cần upload ảnh "
                    + "mới để dùng field này. Nếu vừa gửi field này VỪA gửi "
                    + "\"draftImagePublicIds\" + \"thumbnailImageIndex\", ảnh MỚI upload sẽ "
                    + "được ưu tiên làm thumbnail sau cùng (xử lý draftImagePublicIds sau).",
            example = "42"
    )
    private Integer thumbnailListingImageId;
}
