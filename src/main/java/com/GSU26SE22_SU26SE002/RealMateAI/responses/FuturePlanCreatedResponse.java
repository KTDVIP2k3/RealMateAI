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
public class FuturePlanCreatedResponse {

    private Integer futureInvestmentPlanId;
    private String futureInvestmentPlanName;

    private Integer sourceVersionId;
    private String sourceVersionName;

    private LocalDateTime createdAt;


    @Builder.Default
    private java.util.List<String> skippedItems = new java.util.ArrayList<>();
}
