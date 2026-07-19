package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.CertificationStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ReviewCertificationRequest {

    @NotNull(message = "decision không được để trống (APPROVED hoặc REJECTED)")
    private CertificationStatusEnum decision;

    /** Bắt buộc khi decision = REJECTED (lý do từ chối cho Seller biết). */
    private String reviewerNote;
}
