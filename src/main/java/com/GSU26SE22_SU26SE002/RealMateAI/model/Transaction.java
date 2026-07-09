package com.GSU26SE22_SU26SE002.RealMateAI.model;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor @AllArgsConstructor @Getter
@Setter  @Builder
@Entity @Table(name = "transaction")
public class Transaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    private String checkoutUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type")
    private TransactionTypeEnum transactionType;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "docno_id")
    private Integer docnoId;

    @Column(name = "total_amount")
    private Long totalAmount;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Column(name = "content_description")
    private String contentDescription;

    @Column(name = "transaction_status")
    private String transactionStatus;

    private LocalDateTime createdAt;
}
