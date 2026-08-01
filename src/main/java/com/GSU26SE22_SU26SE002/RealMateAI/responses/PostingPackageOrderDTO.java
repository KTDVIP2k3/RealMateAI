package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostingPackageOrderDTO {
    private Integer postingPackageOrderId;
    private Integer postingPackageId;
    private String postingPackageName;
    private Integer listingId;
    private String listingTitle;
    private BigDecimal totalAmount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer duration;
    private Boolean isActive;
    private String status;
}
