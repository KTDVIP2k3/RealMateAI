package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostingPackageOrderRequest {
    private Integer listingId;
    private Integer postingPackageId;
    private Integer duration;
    private BigDecimal totalAmount;
}
