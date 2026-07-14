package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * POST /listings/price-suggestion
 * Seller nhập thông số tài sản muốn đăng, hệ thống đối chiếu với các tin đăng
 * tương đồng đang hoạt động trên thị trường (cùng loại BĐS + cùng phường/xã)
 * rồi dùng Gemini để đưa ra khoảng giá đề xuất kèm giải thích.
 */
@Data
public class PriceSuggestionRequest {

    @NotNull(message = "Loại bất động sản không được để trống")
    @Schema(example = "1")
    private Integer propertyTypeId;

    @NotNull(message = "Mã phường/xã không được để trống")
    @Schema(example = "26734")
    private String wardCode;

    @NotNull(message = "Diện tích không được để trống")
    @Schema(example = "75.5")
    private Double area;

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
}
