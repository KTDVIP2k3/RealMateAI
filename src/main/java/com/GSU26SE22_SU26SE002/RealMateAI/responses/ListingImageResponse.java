package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingImageResponse {
    private Integer listingImageId;
    private String imageUrl;

    /** true = ảnh đại diện (thumbnail) — luôn được xếp lên đầu danh sách "images". */
    private Boolean isThumbnail;

    private Integer displayOrder;
}
