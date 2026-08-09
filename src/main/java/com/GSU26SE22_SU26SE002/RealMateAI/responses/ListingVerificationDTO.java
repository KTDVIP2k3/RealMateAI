package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.VerificationStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListingVerificationDTO {
    private Integer listingId;
    private String title;
    private String sellerName;
    private LocalDateTime createAt;
    private VerificationStatusEnum verificationStatusEnum;
}
