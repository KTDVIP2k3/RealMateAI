package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteListingResponse {
    private Integer favoriteListingId;
    private LocalDateTime addedAt;

    /** Thông tin bài đăng rút gọn */
    private ListingSummaryResponse listing;
}
