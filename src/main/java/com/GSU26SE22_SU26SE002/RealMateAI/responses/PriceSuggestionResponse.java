package com.GSU26SE22_SU26SE002.RealMateAI.responses;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceSuggestionResponse {

    /** Giá đề xuất (VND), đại diện — nên dùng làm giá trị mặc định gợi ý cho Seller. */
    private Long suggestedPrice;

    /** Khoảng giá hợp lý theo thị trường. */
    private Long minPrice;
    private Long maxPrice;

    /** Đơn giá trung bình VND/m2 tính từ các tin đăng tương đồng. */
    private Long pricePerSqm;

    /** Số lượng tin đăng tương đồng dùng để tham chiếu (cùng loại BĐS + cùng phường/xã). */
    private Integer comparableCount;

    /** true nếu khoảng giá được tính dựa trên dữ liệu thị trường thực tế;
     *  false nếu không đủ dữ liệu tham chiếu và AI chỉ ước lượng theo kiến thức chung. */
    private Boolean basedOnMarketData;

    /** Giải thích ngắn gọn bằng tiếng Việt lý do đề xuất mức giá này. */
    private String reasoning;
}
