package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CreateListingRequest {

    // ── Thông tin Listing (bài đăng thương mại) ───────────────
    @NotBlank(message = "Tiêu đề bài đăng không được để trống")
    private String title;

    @NotBlank(message = "Mô tả bài đăng không được để trống")
    private String description;

    @NotNull(message = "Giá đăng không được để trống")
    @Min(value = 0, message = "Giá phải >= 0")
    private Long price;

    private String contactPerson;
    private String contactPersonName;
    private String contactPersonPhone;
    private String linkSocialContactPerson;
    private String viewingDate;
    private LocalTime startTime;
    private LocalTime endTime;

    // ════════════════════════════════════════════════════════
    //  CHỌN 1 TRONG 2 — xem javadoc class phía trên
    // ════════════════════════════════════════════════════════

    /**
     * ① ĐĂNG LẠI tài sản đã có. Phải thuộc sở hữu Seller hiện tại.
     * Lấy danh sách tài sản qua GET /api/v1/seller/properties.
     */
    private Integer existingPropertyId;

    /**
     * ② TẠO TÀI SẢN MỚI. Object lồng chứa toàn bộ thông số BĐS + Location.
     */
    private CreatePropertyRequest newProperty;

    // ════════════════════════════════════════════════════════
    //  ẢNH — BẮT BUỘC, gửi kèm publicId của ảnh đã upload draft
    // ════════════════════════════════════════════════════════

    /**
     * Danh sách publicId (Cloudinary) của ảnh đã upload ở bước trước qua
     * POST /api/v1/media/upload/multiple?entityType=ACCOUNT&entityId={accountId}.
     *
     * BẮT BUỘC có ít nhất 1 ảnh nếu tạo Property MỚI (newProperty != null).
     * Nếu đăng lại tài sản đã có (existingPropertyId != null) và tài sản đó
     * đã có ảnh từ trước, field này CÓ THỂ để rỗng — ảnh cũ vẫn được giữ lại.
     * Nếu vẫn gửi kèm, ảnh mới sẽ được nối thêm (append) vào bộ ảnh hiện có.
     */
    private List<String> draftImagePublicIds;

    /**
     * Index trong draftImagePublicIds nào sẽ là ảnh đại diện (is_main=true).
     * Chỉ áp dụng nếu đây là lần đầu Property có ảnh; mặc định = 0.
     */
    private Integer mainImageIndex;
}
