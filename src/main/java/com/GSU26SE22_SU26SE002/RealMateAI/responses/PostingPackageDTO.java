package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostingPackageDTO {
    private String name;
    private String description;
    private BigDecimal posting_package_price;
    private BigDecimal  priority;
}
