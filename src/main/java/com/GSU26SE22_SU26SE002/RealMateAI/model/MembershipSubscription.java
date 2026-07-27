package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "membership_subscription")
public class MembershipSubscription {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_subscription_id")
    private Integer membershipSubscriptionId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id")
    private Investor investor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_plan_id", nullable = false)
    private MembershipPlan membershipPlan;

//    private LocalDateTime startDate;
//    private LocalDateTime endDate;
//    private String status;

    @Enumerated(EnumType.STRING)
    private MembershipSubscriptionEnum membershipSubscriptionEnum_status;
    private BigDecimal price_pay;
    private Integer quantity_using;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
