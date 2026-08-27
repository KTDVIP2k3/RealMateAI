package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestmentPortfolioRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileSimpleDTO {
    private Integer investmentProfileId;
    private Integer latestVersionId;
    private Long totalCapital;
    private String name;
    private String consciousName;
    private List<String> wardName;
    private Boolean isActive;
    private Long equity;
    private String strategyName;
    private LocalDateTime createdAt;
}
