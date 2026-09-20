package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OtpRequest {
    @NotBlank(message = "Mã OTP bắt buộc phải bao gồm 6 chữ số")
    @Size(min = 6, max = 6, message = "Mã OTP bắt buộc phải bao gồm 6 chữ số")
    private String otp;
    
    @NotBlank(message = "Email không được để trống")
    private String email;
}
