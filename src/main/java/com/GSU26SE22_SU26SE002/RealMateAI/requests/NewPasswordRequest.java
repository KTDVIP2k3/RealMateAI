package com.GSU26SE22_SU26SE002.RealMateAI.requests;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewPasswordRequest {
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu mới phải dài ít nhất 8 ký tự")
    private String newPassword;
    
    @NotBlank(message = "Email không được để trống")
    private String email;
}
