package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffDashboardKpiDTO {
    private long pendingListingsCount;
    private long pendingAccountVerificationsCount;
    private long pendingListingCertificationsCount;
    private long pendingPropertyValuationsCount;
}
