package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class SubmitCertificationRequest {

    /** Giấy tờ pháp lý tài sản (sổ đỏ/sổ hồng, hợp đồng mua bán...) — bắt buộc ≥ 1 file. */
    private List<MultipartFile> documents;
}

