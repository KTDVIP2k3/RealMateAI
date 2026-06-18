package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class PropertyImageResponse {
    private Integer propertyImageId;
    private String imageUrl;
    private Boolean isMain;
    private Integer displayOrder;
}
