package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyValuationDTO {
    private Integer valuationRequestId;
    private Integer propertyId;
    private String address;
    private LocalDateTime requestedAt;
    private String status;
}