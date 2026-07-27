package com.GSU26SE22_SU26SE002.RealMateAI.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestionResponse {

    /** Gợi ý vị trí — khớp tên Phường/Xã hoặc Tỉnh/Thành. */
    private List<SearchSuggestionItem> locations;

    /** Gợi ý tin đăng/dự án — khớp tiêu đề tin đăng hoặc tên dự án (property.projectName). */
    private List<SearchSuggestionItem> listings;

    /** Gợi ý loại bất động sản — khớp tên PropertyType. */
    private List<SearchSuggestionItem> propertyTypes;

    /** Lịch sử tìm kiếm gần đây CỦA CHÍNH người dùng đang đăng nhập — rỗng nếu chưa đăng nhập. */
    private List<SearchSuggestionItem> recentSearches;
}
