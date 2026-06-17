package com.GSU26SE22_SU26SE002.RealMateAI.requests;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddFavoriteRequest {

    @NotNull(message = "listingId không được để trống")
    private Integer listingId;
}
