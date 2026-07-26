package com.GSU26SE22_SU26SE002.RealMateAI.responses;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response GỌN cho POST /investment-plans/future — CHỈ xác nhận đã tạo +
 * lưu thành công phiên bản kế hoạch tương lai (FUTURE_PLAN), KHÔNG còn trả
 * kèm toàn bộ output (scenarios/executionPlan/portfolios/profitResults/
 * comparison) như trước.
 *
 * Lý do tách: "future output" (scenarios, execution plan, lợi nhuận từng
 * property, so sánh với version gốc...) được SINH RA VÀ GẮN VỚI 1
 * InvestmentProfileVersion cụ thể (chính là "newVersionId" ở đây) — bản
 * chất đây là dữ liệu đọc lại theo ID, không phải dữ liệu chỉ tồn tại đúng
 * lúc tạo. Tách ra giúp:
 *  - FE chỉ cần lưu lại "newVersionId" sau khi tạo, gọi
 *    GET /investment-plans/future/{newVersionId} bất kỳ lúc nào sau đó để
 *    xem/tải lại đúng 1 output đó (màn hình So sánh có thể mở lại nhiều lần
 *    mà không cần tạo lại).
 *  - API tạo không phải cõng thêm việc build lại các DTO lồng nhau nặng nề
 *    (investmentPortfolios/propertyProfitResults) — việc build response chi
 *    tiết chỉ xảy ra đúng 1 nơi (getFuturePlanDetail), tránh 2 nơi cùng lắp
 *    ráp 1 cấu trúc dữ liệu dễ bị lệch nhau (đã từng xảy ra: response tạo
 *    mới có investmentPortfolios/propertyProfitResults/portfolioScore/
 *    comparisonWithSource nhưng GET lại thiếu — đã hợp nhất lại ở
 *    getFuturePlanDetail).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FuturePlanCreatedResponse {

    /** ID version mới vừa tạo — dùng để gọi GET /investment-plans/future/{newVersionId} */
    private Integer newVersionId;
    private String newVersionName;

    private Integer sourceVersionId;
    private String sourceVersionName;

    private LocalDateTime createdAt;

    /**
     * Danh sách mô tả các property investor chọn nhưng KHÔNG resolve được
     * (listingId/manualPropertyId không tồn tại, hoặc actualPurchasePrice
     * <= 0) — TRƯỚC ĐÂY bị bỏ qua ÂM THẦM (silent), khiến GET lại thấy
     * "properties": [] mà không rõ vì sao. Rỗng nếu mọi property đều resolve
     * thành công.
     */
    @Builder.Default
    private java.util.List<String> skippedItems = new java.util.ArrayList<>();
}
