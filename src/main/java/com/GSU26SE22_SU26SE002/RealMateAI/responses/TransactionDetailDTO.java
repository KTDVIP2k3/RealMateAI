package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor @AllArgsConstructor
public class TransactionDetailDTO {
    private String fullName;

    private String phone;

    private TransactionTypeEnum transactionType;

    private LocalDateTime transactionDate;

    private String packageName;

    private Long totalAmount;

    private String transactionCode;

    private String contentDescription;

    private String transactionStatus;

}
