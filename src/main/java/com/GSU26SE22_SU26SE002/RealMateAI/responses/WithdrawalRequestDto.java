package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalRequestDto {
    private Integer withdrawalId;
    private Integer accountId;
    private String accountName;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;
    private LocalDateTime createdAt;
}