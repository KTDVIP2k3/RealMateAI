package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.GenderEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAccountDetailDTO {
    private Integer accountId;
    private String userName;
    private String email;
    private String fullName;
    private String phone;
    private String avatar;
    private GenderEnum gender;
    private RoleEnum role;
    private Boolean isActive;
    private Date birthDate;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    // sub-entity flags
    private Boolean hasInvestorProfile;
    private Boolean hasSellerProfile;
}
