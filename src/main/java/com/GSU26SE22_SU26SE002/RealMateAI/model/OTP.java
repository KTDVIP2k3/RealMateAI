package com.GSU26SE22_SU26SE002.RealMateAI.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "otps")
@Data
public class OTP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int otpId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @OneToOne
    @JoinColumn(name = "account_id", referencedColumnName = "accountId", unique = true)
    private Account account;
}