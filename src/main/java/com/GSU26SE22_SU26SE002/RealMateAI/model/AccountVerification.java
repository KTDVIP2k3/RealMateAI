package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.VerificationStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter @Builder
@Entity @Table(name = "account_verification")
public class AccountVerification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer accountVerificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;
    /** Căn cước công dân mặt trước */
    private String cccdmt;
    /** Căn cước công dân mặt sau */
    private String cccdms;
    private String selfie;
    private String businessLicense;

    @Enumerated(EnumType.STRING)
    private VerificationStatusEnum verificationStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
