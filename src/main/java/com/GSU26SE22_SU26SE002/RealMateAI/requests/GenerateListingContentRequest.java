package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * POST /listings/generate-content
 * Seller nhập các thông số thô của tài sản, AI (Gemini) sinh ra tiêu đề +
 * mô tả bài đăng chuẩn marketing bằng tiếng Việt. Seller có thể chỉnh sửa
 * lại nội dung này trước khi gửi thật vào CreateListingWithNewPropertyRequest.
 */
@Data
public class GenerateListingContentRequest {

    @NotNull(message = "Loại bất động sản không được để trống")
    @Schema(example = "1")
    private Integer propertyTypeId;

    @Schema(example = "26734", description = "Mã phường/xã — dùng để AI nhắc tới vị trí trong bài đăng")
    private String wardCode;

    @NotNull(message = "Diện tích không được để trống")
    @Schema(example = "75.5")
    private Double area;

    @Schema(example = "3500000000")
    private Long price;

    @Schema(example = "2")
    private Integer bedroom;

    @Schema(example = "2")
    private Integer bathroom;

    @Schema(example = "15")
    private Integer floor;

    @Schema(example = "Đông Nam")
    private String direction;

    @Schema(example = "Sổ hồng")
    private String legalStatus;

    @Schema(example = "Gateway Thảo Điền")
    private String projectName;

    @Schema(example = "120 Xa lộ Hà Nội, P.Thảo Điền")
    private String addressParticular;

    @Schema(example = "[\"Hồ bơi\", \"Gần trường quốc tế\", \"Nội thất cao cấp\"]",
            description = "Các điểm nhấn Seller muốn AI nhấn mạnh trong bài đăng")
    private List<String> highlights;

    @Schema(example = "professional", description = "Văn phong: professional (mặc định) | friendly | luxury")
    private String tone;
}
