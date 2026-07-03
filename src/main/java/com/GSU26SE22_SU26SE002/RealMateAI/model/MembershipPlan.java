package com.GSU26SE22_SU26SE002.RealMateAI.model;


import com.GSU26SE22_SU26SE002.RealMateAI.model.MembershipSubscription;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "membership_plan")
public class MembershipPlan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_plan_id")
    private Integer membershipPlanId;

    private String name;
//    private Integer duration;

    @Column(columnDefinition = "Text")
    private String description;
    private Integer quantity;
    private BigDecimal price;
    private Boolean isActive;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "membershipPlan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MembershipSubscription> membershipSubscriptions = new ArrayList<>();
}
