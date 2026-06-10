package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class CreateAccountRequest {
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
}
