package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyValuationResponse {

    private Integer propertyValuationId;
    private Integer propertyId;
    private String propertyTitle;
    private String status;
    private String addressParticular;
    private String sellerNote;

    private Long landPrice;
    private Long constructionCost;
    private Long totalValue;

    private String reason;
    private String reviewedByName;

    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}