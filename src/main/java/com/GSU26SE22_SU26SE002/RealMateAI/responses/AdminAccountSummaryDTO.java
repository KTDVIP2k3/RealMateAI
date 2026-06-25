package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAccountSummaryDTO {
    private Integer accountId;
    private String userName;
    private String email;
    private String fullName;
    private String phone;
    private RoleEnum role;
    private Boolean isActive;
    private LocalDateTime createAt;
}
