package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class MembershipPlanRequest {
    private String name;
    private String description;
    private Integer quantity;
    private BigDecimal price;
}