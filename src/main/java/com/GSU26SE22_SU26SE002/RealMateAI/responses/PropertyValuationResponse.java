package com.GSU26SE22_SU26SE002.RealMateAI.responses;


import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PropertyValuationResponse {
    private Integer propertyValuationId;
    private Integer propertyId;
    private String propertyTitle;
    private String status;
    private String sellerNote;
    private Long totalValue;
    private String reason;
    private Long marketUnitPrice;
    private Long locationK;
    private Long gfa;
    private Long constructionNewPrice;
    private Long remainingQuantity;
    private Long landPrice;
    private Long constructionCost;
    private String reviewedByName;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}

