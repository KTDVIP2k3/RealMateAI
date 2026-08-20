package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyTransactionResponseDto {

    private BigDecimal totalDeposit;
    private BigDecimal totalSpent;

    private List<Map<String, Object>> content;

    private int page;
    private int size;
    private int totalElements;
    private int totalPages;
    private boolean last;
}