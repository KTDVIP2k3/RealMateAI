package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/**
 * Luồng ①: Đăng lại tài sản ĐÃ CÓ SẴN.
 * Gửi bằng application/json (không cần multipart).
 * Ảnh không bắt buộc — nếu property đã có ảnh sẵn thì dùng lại.
 * Nếu muốn thêm ảnh mới thì gửi draftImagePublicIds (upload trước qua Cloudinary).
 */
@Data
public class CreateListingWithExistingPropertyRequest {

    @NotNull(message = "existingPropertyId không được để trống")
    private Integer existingPropertyId;

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

    // ── Ảnh ─────────────────────────────────────────────────────────────────

    @Schema(
            description = "OPTIONAL. Danh sách publicId ảnh đã upload TRƯỚC đó qua "
                    + "POST /media/upload/multiple (entityType=ACCOUNT). Để trống nếu "
                    + "không thêm ảnh mới (hệ thống tự copy ảnh từ property có sẵn). "
                    + "Thứ tự phần tử trong mảng này quyết định displayOrder của ảnh, "
                    + "và là cơ sở để \"thumbnailImageIndex\" bên dưới trỏ tới.",
            example = "[\"realmateai/listings/abc123\", \"realmateai/listings/def456\"]"
    )
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> draftImagePublicIds;

    @Schema(
            description = "Vị trí (0-based, đếm từ 0) trong mảng \"draftImagePublicIds\" ở "
                    + "trên xác định ẢNH NÀO LÀ THUMBNAIL (ảnh đại diện, isThumbnail=true, "
                    + "luôn được xếp đầu tiên khi GET chi tiết tin đăng). VÍ DỤ: "
                    + "draftImagePublicIds=[\"a\",\"b\",\"c\"] + thumbnailImageIndex=1 "
                    + "→ ảnh \"b\" là thumbnail. Mặc định = 0 (ảnh đầu tiên) nếu không truyền. "
                    + "CHỈ có tác dụng khi Listing CHƯA có ảnh nào (lần đầu thêm ảnh) — nếu "
                    + "Listing đã có ảnh, ảnh mới thêm vào sẽ KHÔNG tự đổi thumbnail hiện tại.",
            example = "0",
            defaultValue = "0"
    )
    private Integer thumbnailImageIndex;
}
