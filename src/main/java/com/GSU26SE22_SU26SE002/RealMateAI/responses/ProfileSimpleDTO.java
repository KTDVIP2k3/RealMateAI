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
public class ProfileSimpleDTO {
    private Integer investmentProfileId;
    private String name;
    private String conscious;
    private String ward;
    private Boolean isActive;
    private Long equity;
    private Long expectedRoi;
    private Long durationYear;
    private String strategyName;
    private LocalDateTime createdAt;
}
