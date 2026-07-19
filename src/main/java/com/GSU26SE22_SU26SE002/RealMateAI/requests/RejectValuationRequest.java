package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RejectValuationRequest {

    @NotBlank(message = "reason không được để trống")
    private String reason;
}
