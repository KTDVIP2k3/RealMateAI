package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.GenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class AdminCreateAccountRequest {

    @Schema(description = "Tên đăng nhập", example = "nguyen.van.a")
    private String userName;

    @Schema(description = "Mật khẩu", example = "Password@123")
    private String password;

    @Schema(description = "Email", example = "nguyen.van.a@gmail.com")
    private String email;

    @Schema(description = "Họ và tên", example = "Nguyễn Văn A")
    private String fullName;

    @Schema(description = "Số điện thoại", example = "0901234567")
    private String phone;

    @Schema(type = "string", format = "date", example = "1990-01-15")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date birthDate;

    @Schema(implementation = String.class, allowableValues = {"Male", "Female"})
    private GenderEnum gender;
}
