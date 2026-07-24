package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PostingPackageCategoryDetailDTO {
    private Integer postingPackageCategoryId;

    private String postingPackageCategoryName;

    private String description;

    private BigDecimal priority;

//    private Boolean isActive;
//    private Boolean isDeleted;
//
//
//    private LocalDateTime createdAt;
//
//    private LocalDateTime updatedAt;
}
