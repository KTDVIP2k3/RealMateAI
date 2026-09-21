package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class MembershipPlanRequest {
//    @NotBlank(message = "Tên gói thành viên không được để trống")
    private String name;

    @NotBlank
    private String description;
    
//    @NotNull(message = "Số lượng lượt cấp thêm không được để trống")
//    @Min(value = 1, message = "Số lượng lượt cấp thêm phải lớn hơn 0")
    private Integer quantity;
    
//    @NotNull(message = "Giá gói thành viên không được để trống")
//    @Min(value = 0, message = "Giá gói thành viên không được là số âm")
    private BigDecimal price;
}