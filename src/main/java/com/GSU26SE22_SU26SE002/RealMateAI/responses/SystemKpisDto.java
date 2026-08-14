package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemKpisDto {
    @JsonProperty("total_accounts")
    private Long totalAccounts;

    @JsonProperty("sellers_count")
    private Long sellersCount;

    @JsonProperty("investors_count")
    private Long investorsCount;

    @JsonProperty("staffs_count")
    private Long staffsCount;

    @JsonProperty("total_listings_on_platform")
    private Long totalListingsOnPlatform;

    @JsonProperty("active_listings_count")
    private Long activeListingsCount;
}