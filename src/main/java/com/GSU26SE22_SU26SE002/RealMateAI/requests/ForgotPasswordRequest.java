package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Vui lòng nhập đúng định dạng Email")
    @Email(message = "Định dạng Email không hợp lệ")
    private String email;
}
