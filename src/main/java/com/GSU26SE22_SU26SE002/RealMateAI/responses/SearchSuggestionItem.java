package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionItem {

    @Schema(description = "Loại gợi ý", allowableValues = {"LOCATION", "LISTING", "PROPERTY_TYPE", "RECENT_SEARCH"})
    private String type;

    @Schema( example = "Vinhome Grand Park Q9")
    private String label;

    @Schema(description = "Giá trị điền vào ô tìm kiếm (q) khi user chọn gợi ý này")
    private String value;

    @Schema(description = "Mã tham chiếu tuỳ loại: wardCode/provinceCode (LOCATION), listingId (LISTING), propertyTypeId (PROPERTY_TYPE); null với RECENT_SEARCH")
    private String code;
}
