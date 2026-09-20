package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String userName;
    
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
