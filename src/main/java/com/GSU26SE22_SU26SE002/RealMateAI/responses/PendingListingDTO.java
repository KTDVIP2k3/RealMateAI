package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingListingDTO {
    private Long listingId;
    private String title;
    private String sellerName;
    private LocalDateTime createdAt;
    private String verificationStatus;
}