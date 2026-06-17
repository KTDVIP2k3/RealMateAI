package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PropertyDetailResponse {
    private Integer propertyId;
    private String title;
    private String description;
    private Double area;
    private Long price;
    private Integer floor;
    private Integer bedroom;
    private Integer bathroom;
    private String direction;

    // Loại & tình trạng
    private String propertyTypeName;
    private String propertyConditionName;

    // Location
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String postalCode;
    private String wardCode;

    // Ảnh thực tế tài sản
    private List<PropertyImageResponse> images;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
