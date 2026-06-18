package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * CreatePropertyRequest — Thông số ĐẦY ĐỦ để tạo MỘT TÀI SẢN MỚI.
 *
 * Đây là sub-object riêng biệt, CHỈ tồn tại và CHỈ được validate khi
 * CreateListingRequest.newProperty != null (tức Seller chọn "tạo tài sản mới"
 * thay vì dùng lại tài sản đã có). Việc tách riêng thành object lồng nhau
 * (thay vì để chung field phẳng với propertyId) giúp Swagger/FE phân biệt
 * RÕ RÀNG 2 luồng nghiệp vụ khác nhau, tránh nhầm lẫn "vừa gửi propertyId
 * vừa gửi propertyTitle thì hệ thống hiểu sao".
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CreatePropertyRequest {

    @NotBlank(message = "Tiêu đề tài sản không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Giá tài sản không được để trống")
    private Long price;

    @NotNull(message = "Diện tích không được để trống")
    private Double area;

    private Integer floor;
    private Integer bedroom;
    private Integer bathroom;
    private String direction;

    private String legalStatus;

    private String addressParticular;

    private String projectName;

    @NotNull(message = "Loại bất động sản không được để trống")
    private Integer propertyTypeId;

    private Integer propertyConditionId;

    private Map<String, Object> propertyAttribute;

    private List<String> propertyPurpose;

    @NotNull(message = "Vĩ độ (latitude) không được để trống")
    private BigDecimal latitude;

    @NotNull(message = "Kinh độ (longitude) không được để trống")
    private BigDecimal longitude;

    private String postalCode;

    @NotBlank(message = "Mã phường/xã không được để trống")
    private String wardCode;


}
