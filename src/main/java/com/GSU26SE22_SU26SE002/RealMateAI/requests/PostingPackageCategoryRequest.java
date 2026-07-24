package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class PostingPackageCategoryRequest {
    private String postingPackageCategoryName;
    private String description;
    private BigDecimal priority;
}
