package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostingPackageRequest {
    private Integer postingPackageCategoryId;
    
    @NotBlank(message = "Tên gói đăng bài không được để trống")
    private String name;
    
    private String description;
    
    @NotNull(message = "Giá gói đăng bài không được để trống")
    @Min(value = 0, message = "Giá gói đăng bài không được là số âm")
    private BigDecimal posting_package_price;
    
    @NotNull(message = "Thời hạn sử dụng không được để trống")
    @Min(value = 1, message = "Thời hạn sử dụng phải lớn hơn 0 ngày")
    private BigDecimal duration;
}
