package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteValuationRequest {
    @NotNull(message = "Đơn giá đất định giá không được để trống")
    @Min(value = 0, message = "Đơn giá đất định giá không được là số âm")
    private Long landPrice;
    
    @NotNull(message = "Chi phí xây dựng không được để trống")
    @Min(value = 0, message = "Chi phí xây dựng không được là số âm")
    private Long constructionCost;
    
    private String reason;
}