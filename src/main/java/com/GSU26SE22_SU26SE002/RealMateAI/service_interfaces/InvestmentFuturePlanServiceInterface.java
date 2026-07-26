package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;


import com.GSU26SE22_SU26SE002.RealMateAI.requests.GenerateFuturePlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface InvestmentFuturePlanServiceInterface {

    /**
     * Nhận feedback property thực tế từ investor -> tạo {@link com.GSU26SE22_SU26SE002.RealMateAI.model.FutureInvestmentPlan}
     * MỚI (bảng RIÊNG, tách hoàn toàn khỏi InvestmentProfileVersion — sourceVersion
     * = version gốc BÌNH THƯỜNG làm baseline, không thể là 1 future-plan khác,
     * nên "không tạo future từ future" đảm bảo ngay ở tầng thiết kế dữ liệu) ->
     * tính lợi nhuận thực tế -> gọi AI sinh kịch bản + so sánh -> lưu toàn bộ
     * trong 1 transaction. Tên tự sinh "Kết quả dự đoán N" (đếm theo investment
     * profile) nếu investor không tự đặt planName.
     *
     * Response CHỈ trả về {@link com.GSU26SE22_SU26SE002.RealMateAI.responses.FuturePlanCreatedResponse}
     * (newVersionId + tên/thông tin cơ bản + skippedItems nếu có property bị bỏ
     * qua) — KHÔNG còn trả kèm toàn bộ output (scenarios/executionPlan/
     * portfolios/profitResults/comparison) như trước.
     * FE dùng "newVersionId" nhận được gọi tiếp GET /investment-plans/future/{newVersionId}
     * để lấy đầy đủ output — vì output này vốn dĩ GẮN VỚI 1 future-plan cụ thể
     * (đọc lại theo ID bất kỳ lúc nào), tách khỏi bước tạo giúp không phải lắp
     * ráp cùng 1 cấu trúc dữ liệu ở 2 nơi khác nhau (dễ bị lệch nhau).
     *
     * Đây CHÍNH LÀ một thao tác "tạo version mới", tương tự updateExistingInvestmentPlan,
     * nên không cần endpoint "confirm" riêng — investor xác nhận ở FE trước khi gọi API này.
     */
    ResponseEntity<ApiResponse> generateAndSaveFuturePlan(GenerateFuturePlanRequest request);

    /**
     * GET /investment-plans/future/{futurePlanId} — nguồn DUY NHẤT để lấy đầy đủ
     * "future output" (scenarios, executionPlan, investmentPortfolios,
     * propertyProfitResults, portfolioScore, comparisonWithSource), tái dựng
     * lại hoàn toàn từ dữ liệu đã lưu ở {@link com.GSU26SE22_SU26SE002.RealMateAI.model.FutureInvestmentPlan}
     * (không phụ thuộc response lúc tạo). Dùng cho cả 2 trường hợp: FE gọi ngay
     * sau khi tạo (dùng newVersionId từ generateAndSaveFuturePlan), hoặc mở lại
     * xem lịch sử future plan cũ.
     */
    ResponseEntity<ApiResponse> getFuturePlanDetail(Integer futurePlanId);

    /**
     * GET /investment-plans/future/by-source/{sourceVersionId} — bước GIỮA
     * trong chuỗi điều hướng: (1) GET /investment-plans/{profileId}/versions
     * lấy danh sách version gốc (investmentProfileVersionId) -> (2) API NÀY,
     * truyền đúng 1 sourceVersionId gốc, lấy danh sách TÓM TẮT các future-plan
     * (bảng FutureInvestmentPlan riêng) phái sinh từ nó (field futureVersionId,
     * KHÁC tên với investmentProfileVersionId ở bước 1 để FE không nhầm lẫn) ->
     * (3) GET /investment-plans/future/{futureVersionId} lấy chi tiết đầy đủ 1
     * future-plan cụ thể.
     */
    ResponseEntity<ApiResponse> getFutureVersionsBySourceVersionId(Integer sourceVersionId);
}
