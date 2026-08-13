package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.GSU26SE22_SU26SE002.RealMateAI.model.WalletWithdrawal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletWithDrawlListDTO {
    private Integer walletWithDrawlId;
    private String bankName;
    private String bankAccountNumber;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createAt;
}
