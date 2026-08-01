package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * MỚI: DTO nhóm theo loại BĐS RIÊNG cho Future Plan (tách khỏi
 * {@link PortfolioAllocationDTO} vốn đang dùng CHUNG với
 * InvestmentPlanServiceImplement). Giữ nguyên cấu trúc, chỉ đổi kiểu
 * `properties` sang {@link FuturePortfolioAllocationPropertyDTO}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuturePortfolioAllocationDTO {
    private String propertyTypeName;
    private List<FuturePortfolioAllocationPropertyDTO> properties;
}
