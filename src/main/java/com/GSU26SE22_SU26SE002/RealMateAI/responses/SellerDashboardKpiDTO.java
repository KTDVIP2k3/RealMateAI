package com.GSU26SE22_SU26SE002.RealMateAI.responses;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardKpiDTO {
    private long totalListings;
    private long activeListings;
    private long hiddenListings;
    private long pendingApproval;
    private long totalViews;
    private long totalSaved;
    private long totalContacts;
}
