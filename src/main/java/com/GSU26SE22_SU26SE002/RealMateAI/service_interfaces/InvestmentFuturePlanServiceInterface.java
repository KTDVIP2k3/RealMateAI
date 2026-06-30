package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;


import com.GSU26SE22_SU26SE002.RealMateAI.requests.GenerateFuturePlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface InvestmentFuturePlanServiceInterface {

    /**
     * Nhận feedback property thực tế từ investor -> tạo InvestmentProfileVersion mới
     * (đánh dấu version = "FUTURE_PLAN", baseVersion = sourceVersion) -> tính lợi nhuận
     * thực tế -> gọi AI sinh kịch bản + so sánh -> lưu toàn bộ trong 1 transaction -> trả
     * kết quả về FE để hiển thị.
     *
     * Đây CHÍNH LÀ một thao tác "tạo version mới", tương tự updateExistingInvestmentPlan,
     * nên không cần endpoint "confirm" riêng — investor xác nhận ở FE trước khi gọi API này.
     */
    ResponseEntity<ApiResponse> generateAndSaveFuturePlan(GenerateFuturePlanRequest request);

    /**
     * Lấy chi tiết version FUTURE_PLAN theo versionId, dùng cho cả 2 trường hợp:
     * FE reload trang, hoặc xem lại lịch sử các future plan cũ.
     * Tái sử dụng đúng cấu trúc dữ liệu của getInvestmentPlanDetailByVersionId hiện có,
     * chỉ bổ sung thêm phần profitSummary.
     */
    ResponseEntity<ApiResponse> getFuturePlanDetail(Integer versionId);
}
