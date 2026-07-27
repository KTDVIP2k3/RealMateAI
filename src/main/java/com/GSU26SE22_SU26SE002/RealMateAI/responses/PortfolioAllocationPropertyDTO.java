package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioAllocationPropertyDTO {
    @JsonIgnore
    private Integer portfolioAllocationPropertyId;
    private String propertyProjectName;
    private Integer area;
    private Double valuePrice;
    private String description;

    /** MỚI: SYSTEM (chọn từ listing AI đề xuất) | MANUAL (investor tự chọn theo property_id có sẵn). */
    private String propertySource;
}
