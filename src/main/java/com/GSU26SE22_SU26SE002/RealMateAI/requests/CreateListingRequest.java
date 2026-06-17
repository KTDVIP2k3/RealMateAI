package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
public class CreateListingRequest {
    // ── Thông tin Listing (bài đăng) ──────────────────────
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

    // ── Chế độ "đăng lại tài sản đã có" ────────────────────
    /**
     * Nếu khác null: Listing mới sẽ liên kết tới Property này
     * (phải thuộc Seller hiện tại). Toàn bộ field Property/Location dưới đây bị bỏ qua.
     */
    private Integer propertyId;

    // ── Thông số Property (chỉ dùng khi propertyId == null) ──
    @NotBlank(message = "Tiêu đề tài sản không được để trống", groups = NewProperty.class)
    private String propertyTitle;

    private String propertyDescription;

    @NotNull(message = "Giá tài sản không được để trống", groups = NewProperty.class)
    private Long propertyPrice;

    @NotNull(message = "Diện tích không được để trống", groups = NewProperty.class)
    private Double area;

    private Integer floor;
    private Integer bedroom;
    private Integer bathroom;
    private String direction;

    @NotNull(message = "Loại bất động sản không được để trống", groups = NewProperty.class)
    private Integer propertyTypeId;

    private Integer propertyConditionId;

    // ── Location (chỉ dùng khi propertyId == null) ────────
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String postalCode;
    private Integer wardCode;

    /**
     * Validation group — dùng nếu muốn enforce @NotNull/@NotBlank các field Property
     * chỉ khi propertyId == null. Hiện service tự kiểm tra thủ công nên group này
     * chỉ mang tính khai báo, không bắt buộc cấu hình @Validated(NewProperty.class).
     */
    public interface NewProperty {}
}
