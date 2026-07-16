package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import org.springframework.web.multipart.MultipartFile;
import lombok.Data;

@Data
public class AccountVerificationRequest {

    private MultipartFile cccdmt;

    private MultipartFile cccdms;

    private MultipartFile selfie;

    private MultipartFile businessLicense;
}