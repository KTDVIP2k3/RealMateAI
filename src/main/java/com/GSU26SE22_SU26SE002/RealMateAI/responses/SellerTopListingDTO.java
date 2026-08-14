package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerTopListingDTO {
    private Integer listingId;
    private String title;
    private Long price;
    private long viewCount;
    private long saveCount;
    private String status;
}
