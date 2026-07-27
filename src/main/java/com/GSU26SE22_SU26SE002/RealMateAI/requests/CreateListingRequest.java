package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Schema(description = "API tạo tin đăng duy nhất — set reuseExistingProperty=true để dùng lại tài sản có sẵn, false để tạo tài sản mới cùng lúc.")
@Data
public class CreateListingRequest {

    @NotNull(message = "reuseExistingProperty không được để trống (true = dùng tài sản có sẵn, false = tạo tài sản mới)")
    @Schema(description = "true: đăng lại tài sản ĐÃ CÓ SẴN (cần existingPropertyId). false: tạo tài sản MỚI (cần các field prop*).", example = "true")
    private Boolean reuseExistingProperty;

    // ── Chỉ dùng khi reuseExistingProperty = true ────────────────────────────
    @Schema(description = "Bắt buộc khi reuseExistingProperty=true. Id tài sản đã có sẵn thuộc sở hữu của Seller.", example = "12")
    private Integer existingPropertyId;

    // ── Listing fields (dùng chung cho cả 2 luồng) ───────────────────────────
    @NotBlank(message = "Tiêu đề bài đăng không được để trống")
    @Schema(example = "Căn hộ 2PN view sông - Quận 7")
    private String title;

    @NotBlank(message = "Mô tả bài đăng không được để trống")
    @Schema(example = "Nội thất đầy đủ, tầng 15, view sông Sài Gòn thoáng mát")
    private String description;

    @NotNull(message = "Giá đăng không được để trống")
    @Min(value = 0, message = "Giá phải >= 0")
    @Schema(example = "3500000000")
    private Long price;

    @Schema(example = "owner")
    private String contactPerson;

    @Schema(example = "0901234567")
    private String contactPersonPhone;

    @Schema(description = "Email liên hệ RIÊNG cho tin đăng này (optional). Để trống → dùng email tài khoản Seller.", example = "seller-contact@gmail.com")
    private String contactEmail;

    @Schema(example = "2025-08-01")
    private String viewingDate;

    @Schema(example = "09:00:00", type = "string")
    private LocalTime startTime;

    @Schema(example = "17:00:00", type = "string")
    private LocalTime endTime;

    // ── Property fields (BẮT BUỘC khi reuseExistingProperty = false) ─────────
    @Schema(example = "Căn hộ Gateway Thảo Điền")
    private String propTitle;

    @Schema(example = "Căn hộ cao cấp, nội thất nhập khẩu")
    private String propDescription;

    @Schema(example = "4200000000")
    private Long propPrice;

    @Schema(example = "75.5")
    private Double propArea;

    @Schema(example = "15")
    private Integer propFloor;

    @Schema(example = "2")
    private Integer propBedroom;

    @Schema(example = "2")
    private Integer propBathroom;

    @Schema(example = "Đông Nam")
    private String propDirection;

    @Schema(example = "Sổ hồng")
    private String propLegalStatus;

    @Schema(example = "120 Xa lộ Hà Nội, P.Thảo Điền")
    private String propAddressParticular;

    @Schema(example = "Gateway Thảo Điền")
    private String propProjectName;

    @Schema(example = "1")
    private Integer propPropertyTypeId;

    @Schema(example = "1")
    private Integer propPropertyConditionId;

    @Schema(example = "10.8039")
    private BigDecimal propLatitude;

    @Schema(example = "106.7346")
    private BigDecimal propLongitude;

    @Schema(example = "700000")
    private String propPostalCode;

    @Schema(example = "26734")
    private String propWardCode;

    @Schema(description = "Nội thất — mô tả tự do", example = "Đầy đủ nội thất")
    private String propFurniture;

    @Schema(description = "OPTIONAL — truyền để tự động thanh toán ngay khi tạo tin. Để trống nếu muốn thanh toán sau.", example = "19")
    private Integer postingPackageId;

    @Schema(description = "Bắt buộc kèm postingPackageId. Số ngày đăng tin.", example = "30")
    private Integer duration;

    @Schema(description = "Bắt buộc kèm postingPackageId. Số tiền thanh toán (khớp giá gói).", example = "1111.00")
    private BigDecimal totalAmount;

    // ── Ảnh (đã upload TRƯỚC qua POST /media/upload/multiple) ───────────────
    @Schema(
            description = "Danh sách publicId ảnh đã upload TRƯỚC đó qua "
                    + "POST /media/upload/multiple (entityType=ACCOUNT). BẮT BUỘC có ít "
                    + "nhất 1 phần tử khi reuseExistingProperty=false (tài sản mới chưa "
                    + "có ảnh sẵn). Khi reuseExistingProperty=true, để trống nếu không "
                    + "thêm ảnh mới (hệ thống tự copy ảnh từ property có sẵn). Thứ tự "
                    + "phần tử quyết định displayOrder, và là cơ sở để "
                    + "\"thumbnailImageIndex\" bên dưới trỏ tới.",
            example = "[\"realmateai/listings/abc123\", \"realmateai/listings/def456\"]"
    )
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> draftImagePublicIds;

    @Schema(
            description = "Vị trí (0-based) trong \"draftImagePublicIds\" xác định ẢNH NÀO "
                    + "LÀ THUMBNAIL. Mặc định = 0 (ảnh đầu tiên) nếu không truyền.",
            example = "0",
            defaultValue = "0"
    )
    private Integer thumbnailImageIndex;
}
