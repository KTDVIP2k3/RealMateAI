package com.GSU26SE22_SU26SE002.RealMateAI.requests;

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

    /**
     * Thêm ảnh mới cho property (optional).
     * Nếu property đã có ảnh, field này để null hoặc rỗng.
     */
    private List<String> draftImagePublicIds;
    private Integer mainImageIndex;
}