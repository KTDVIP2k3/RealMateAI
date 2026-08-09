package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "investment_profile")
public class InvestmentProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "investment_profile_id")
    private Integer investmentProfileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investor_id")
    @JsonIgnore
    private Investor investor;

    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Boolean isActive;

    @OneToMany(mappedBy = "investmentProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<InvestmentProfileVersion> profileVersions = new ArrayList<>();
}
