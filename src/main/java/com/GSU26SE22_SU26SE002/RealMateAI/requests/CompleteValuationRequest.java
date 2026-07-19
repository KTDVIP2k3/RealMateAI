package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CompleteValuationRequest {

    @NotNull
    @Positive
    private Long totalValue;

    /** Căn cứ/nhận định của Staff khi đưa ra mức giá này. */
    private String reason;

    private Long marketUnitPrice;
    private Long locationK;
    private Long gfa;
    private Long constructionNewPrice;
    private Long remainingQuantity;
    private Long landPrice;
    private Long constructionCost;
}
