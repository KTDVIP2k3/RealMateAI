package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountVerificationDTOV2 {
    private Integer verificationId;
    private Integer accountId;
    private String fullName;
    private String role;
    private LocalDateTime submittedAt;
    private String status;
}