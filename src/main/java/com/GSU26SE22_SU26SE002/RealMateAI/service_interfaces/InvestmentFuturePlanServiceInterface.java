package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;


import com.GSU26SE22_SU26SE002.RealMateAI.requests.GenerateFuturePlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;

public interface InvestmentFuturePlanServiceInterface {

    /**
     * Nhận feedback property thực tế từ investor -> tạo InvestmentProfileVersion mới
     * (đánh dấu version = "FUTURE_PLAN", baseVersion = sourceVersion) -> tính lợi nhuận
     * thực tế -> gọi AI sinh kịch bản + so sánh -> lưu toàn bộ trong 1 transaction.
     *
     * Response CHỈ trả về {@link com.GSU26SE22_SU26SE002.RealMateAI.responses.FuturePlanCreatedResponse}
     * (newVersionId + tên/thông tin cơ bản) — KHÔNG còn trả kèm toàn bộ output
     * (scenarios/executionPlan/portfolios/profitResults/comparison) như trước.
     * FE dùng "newVersionId" nhận được gọi tiếp GET /investment-plans/future/{newVersionId}
     * để lấy đầy đủ output — vì output này vốn dĩ GẮN VỚI 1 version cụ thể (đọc lại
     * theo ID bất kỳ lúc nào), tách khỏi bước tạo giúp không phải lắp ráp cùng 1
     * cấu trúc dữ liệu ở 2 nơi khác nhau (dễ bị lệch nhau).
     *
     * Đây CHÍNH LÀ một thao tác "tạo version mới", tương tự updateExistingInvestmentPlan,
     * nên không cần endpoint "confirm" riêng — investor xác nhận ở FE trước khi gọi API này.
     */
    ResponseEntity<ApiResponse> generateAndSaveFuturePlan(GenerateFuturePlanRequest request);

    /**
     * GET /investment-plans/future/{versionId} — nguồn DUY NHẤT để lấy đầy đủ
     * "future output" (scenarios, executionPlan, investmentPortfolios,
     * propertyProfitResults, portfolioScore, comparisonWithSource), tái dựng
     * lại hoàn toàn từ dữ liệu đã lưu ở DB (không phụ thuộc response lúc tạo).
     * Dùng cho cả 2 trường hợp: FE gọi ngay sau khi tạo (dùng newVersionId từ
     * generateAndSaveFuturePlan), hoặc mở lại xem lịch sử future plan cũ.
     */
    @Transactional
    ResponseEntity<ApiResponse> getFuturePlanDetail(Integer versionId);

    /**
     * GET /investment-plans/future/by-source/{sourceVersionId} — bước GIỮA
     * trong chuỗi điều hướng: (1) GET /investment-plans/{profileId}/versions
     * lấy danh sách version gốc (investmentProfileVersionId) -> (2) API NÀY,
     * truyền đúng 1 sourceVersionId gốc, lấy danh sách TÓM TẮT các FUTURE_PLAN
     * version phái sinh từ nó (field futureVersionId, KHÁC tên với
     * investmentProfileVersionId ở bước 1 để FE không nhầm lẫn) -> (3) GET
     * /investment-plans/future/{futureVersionId} lấy chi tiết đầy đủ 1
     * future-version cụ thể.
     */
    ResponseEntity<ApiResponse> getFutureVersionsBySourceVersionId(Integer sourceVersionId);
}
