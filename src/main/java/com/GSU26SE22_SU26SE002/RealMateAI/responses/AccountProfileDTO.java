package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.GenderEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountProfileDTO {
    private Integer accountId;
    private String userName;
    private String password;
    private String email;
    private String full_name;
    private String phone;
    private String avatar;
    private GenderEnum genderEnum;
    private RoleEnum roleEnum;

}
