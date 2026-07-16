package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.springframework.web.multipart.MultipartFile;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({
        "verificationId",
        "cccdmt",
        "cccdms",
        "selfie",
        "businessLicense"
})
public class AccountVerificationUpdateRequest extends AccountVerificationRequest {

    private Long verificationId;

    private MultipartFile cccdmt;

    private MultipartFile cccdms;

    private MultipartFile selfie;

    private MultipartFile businessLicense;
}