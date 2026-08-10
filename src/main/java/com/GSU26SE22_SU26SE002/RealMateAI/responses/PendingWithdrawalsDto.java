package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingWithdrawalsDto {
    @JsonProperty("total_pending_amount")
    private BigDecimal totalPendingAmount;

    private List<WithdrawalRequestDto> requests;
}