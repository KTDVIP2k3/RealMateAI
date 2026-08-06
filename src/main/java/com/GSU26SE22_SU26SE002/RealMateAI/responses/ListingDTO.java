package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingDTO {

    private Integer listingId;
    private String title;
    private Long price;
    private Double area;
    private Integer bedroom;
    private Integer bathroom;
    private String propertyTypeName;
    private String thumbnailUrl;

    @JsonProperty("isActive")
    private Boolean isActive;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer viewCount;
    private LocalDateTime createdAt;

    @JsonProperty("isFavorited")
    private Boolean isFavorited;

    private String verificationStatus;

    @JsonProperty("isVerified")
    private Boolean isVerified;
}