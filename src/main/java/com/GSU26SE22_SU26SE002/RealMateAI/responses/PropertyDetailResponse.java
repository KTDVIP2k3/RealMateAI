package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


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

    private String legalStatus;
    private String addressParticular;
    private String projectName;
    private Map<String, Object> propertyAttribute;
    private Map<String, Object>  propertyPurpose;

    // Loại & tình trạng
    private String propertyTypeName;
    private String propertyConditionName;

    // Location
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String postalCode;
    private String wardCode;
    private String wardName;

    // Property KHÔNG còn giữ ảnh — ảnh nay thuộc về Listing (xem
    // ListingDetailResponse#images / ListingSummaryResponse#thumbnailUrl).

    private Boolean isActive;

    /**
     * Số lượng Listing (cả đang active và đang chờ duyệt) hiện đang
     * tham chiếu tới Property này — giúp Seller biết tài sản này
     * đã/đang được đăng bao nhiêu lần khi xem GET /seller/properties.
     */
    private Integer activeListingCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
