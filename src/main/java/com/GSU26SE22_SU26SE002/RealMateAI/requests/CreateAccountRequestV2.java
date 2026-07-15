package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.GenderEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAccountRequestV2 {
    private String userName;
    private String password;
    private String email;
    private String fullName;
    private String phone;

//
//    private MultipartFile avatar;


    @Schema(
            type = "string",
            format = "date",
            example = "2003-08-27"
    )
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthDate;


    @Schema(implementation = String.class, allowableValues = {"Male", "Female"})
    private GenderEnum gender;


    @Schema(implementation = String.class, allowableValues = {"Investor", "Seller", "Staff", "Admin"})
    private RoleEnum role;
}
