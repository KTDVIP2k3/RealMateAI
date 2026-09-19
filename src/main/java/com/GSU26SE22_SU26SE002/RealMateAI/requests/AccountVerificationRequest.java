package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import org.springframework.web.multipart.MultipartFile;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class AccountVerificationRequest {

    @NotNull(message = "Phải tải lên đầy đủ ảnh 2 mặt CCCD")
    private MultipartFile cccdmt;

    @NotNull(message = "Phải tải lên đầy đủ ảnh 2 mặt CCCD")
    private MultipartFile cccdms;

    @NotNull(message = "Phải tải lên ảnh chụp khuôn mặt")
    private MultipartFile selfie;

//    private MultipartFile businessLicense;
}