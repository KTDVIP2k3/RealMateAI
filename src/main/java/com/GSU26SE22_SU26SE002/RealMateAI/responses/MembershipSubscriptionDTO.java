package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MembershipSubscriptionDTO {
    private Integer membershipSubscriptionId;
    private MembershipSubscriptionEnum membershipSubscriptionEnum_status;
    private BigDecimal price_pay;
    private Integer quantity_using;
    private boolean isActive;
}
