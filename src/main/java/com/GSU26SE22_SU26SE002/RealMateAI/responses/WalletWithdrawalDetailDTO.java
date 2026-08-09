package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor @AllArgsConstructor
public class WalletWithdrawalDetailDTO {
    private Integer walletWithDrawlId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createAt;
    private String note;
    private String rejectReason;
    private String fullName;
}
