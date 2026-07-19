package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificationRequestResponse {
    private Integer certificationRequestId;
    private Integer listingId;
    private String listingTitle;
    private Integer sellerId;
    private String status;
    private String reviewerNote;
    private List<String> documentUrls;
    private String reviewedByName;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
