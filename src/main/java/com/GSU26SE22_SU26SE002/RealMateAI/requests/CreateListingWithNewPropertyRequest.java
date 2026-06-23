package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;

/**
 * Luồng ②: Tạo tài sản MỚI + đăng tin cùng lúc.
 * Gửi bằng multipart/form-data — tất cả field đều là form field phẳng (không nested).
 * Ảnh gửi qua part "images" (List<MultipartFile>).
 *
 * LƯU Ý: Dùng camelCase với prefix "prop" thay vì "prop_" (dấu gạch dưới).
 * Lý do: @ModelAttribute binding dựa trên chuẩn JavaBean getter (getPropTitle, getPropPrice, ...),
 * field có dấu _ (prop_title) sinh getter getProp_title() → Spring KHÔNG bind được.
 */
@Data
public class CreateListingWithNewPropertyRequest {

    // ── Listing fields ───────────────────────────────────────────────────────
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

    @Schema(example = "Nguyễn Văn A")
    private String contactPersonName;

    @Schema(example = "0901234567")
    private String contactPersonPhone;

    @Schema(example = "https://zalo.me/0901234567")
    private String linkSocialContactPerson;

    @Schema(example = "2025-08-01")
    private String viewingDate;

    @Schema(example = "09:00:00", type = "string")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private String startTime;

    @Schema(example = "17:00:00", type = "string")
    @DateTimeFormat(pattern = "HH:mm:ss")
    private String endTime;

    // ── Property fields (prefix "prop" — camelCase, không dùng dấu _) ───────
    @NotBlank(message = "Tiêu đề tài sản không được để trống")
    @Schema(example = "Căn hộ Gateway Thảo Điền")
    private String propTitle;

    @Schema(example = "Căn hộ cao cấp, nội thất nhập khẩu")
    private String propDescription;

    @NotNull(message = "Giá tài sản không được để trống")
    @Schema(example = "4200000000")
    private Long propPrice;

    @NotNull(message = "Diện tích không được để trống")
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

    @NotNull(message = "Loại bất động sản không được để trống")
    @Schema(example = "1")
    private Integer propPropertyTypeId;

    @JsonProperty("propPropertyConditionId")
    @Schema(example = "1")
    private Integer propPropertyConditionId;

    @NotNull(message = "Vĩ độ không được để trống")
    @Schema(example = "10.8039")
    private BigDecimal propLatitude;

    @NotNull(message = "Kinh độ không được để trống")
    @Schema(example = "106.7346")
    private BigDecimal propLongitude;

    @Schema(example = "700000")
    private String propPostalCode;

    @NotBlank(message = "Mã phường/xã không được để trống")
    @Schema(example = "26734")
    private String propWardCode;

    // ── Ảnh ─────────────────────────────────────────────────────────────────
    @Schema(example = "0")
    private Integer mainImageIndex;
}