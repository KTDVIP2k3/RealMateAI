package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.model.FavoriteListing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.InvestmentProfile;
import com.GSU26SE22_SU26SE002.RealMateAI.model.MembershipSubscription;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Investor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer investorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

     private String linkSocial;
    private String address;
    private String investmentStyle;
    private String investmentExperience;
    private String profitTarget;
    private String managementAbility;
    private String levelOfVolatility;
    private String capitalUtilizationMindset;
    private String positionalPriority;
    private String investmentMethod;
    private String stableIncome;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "investor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InvestmentProfile> investmentProfiles = new ArrayList<>();

    @OneToMany(mappedBy = "investor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MembershipSubscription> membershipSubscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "investor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FavoriteListing> favoriteListings = new ArrayList<>();

}
