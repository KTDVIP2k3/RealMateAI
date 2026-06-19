package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SaveInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestmentPlanServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/investment-plans")
public class InvestmentPlanController {

    @Autowired
    private InvestmentPlanServiceInterface investmentPlanServiceInterface;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('Investor')")
    @Operation(summary = "Investor: Lấy danh sách CHA (InvestmentProfile) - thông số hiển thị lấy từ bản CON (InvestmentProfileVersion) mới nhất")
    public ResponseEntity<ApiResponse> getProfilesByInvest(){
        return investmentPlanServiceInterface.getListProfileByInvestor();
    }

    @GetMapping("/{profileId}/versions")
    @PreAuthorize("hasAnyRole('Investor')")
    @Operation(summary = "Investor: Lấy danh sách các CON (InvestmentProfileVersion) thuộc về CHA (InvestmentProfile) này")
    public ResponseEntity<ApiResponse> getProfilesByInvest(Integer profileId){
        return investmentPlanServiceInterface.getListViewsByProfileId(profileId);
    }

    @GetMapping("/versions/{versionId}/inputs")
    @PreAuthorize("hasAnyRole('Investor')")
    @Operation(summary = "Investor: Xem chi tiết thông số cài đặt đầu vào của CON (InvestmentProfileVersion)")
    public ResponseEntity<ApiResponse> getProfileDetailById(@PathVariable Integer versionId) {
        return investmentPlanServiceInterface.getProfileVersionDetailById(versionId);
    }

    @GetMapping("/versions/{versionId}/results")
    @PreAuthorize("hasAnyRole('Investor')")
    @Operation(summary = "Investor: Xem chi tiết kết quả dòng tiền và giỏ hàng của CON (InvestmentProfileVersion)")
    public ResponseEntity<ApiResponse> getInvestmentPlanDetailByProfileId(@PathVariable Integer versionId) {
        return investmentPlanServiceInterface.getInvestmentPlanDetailByVersionId(versionId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Investor')")
    @Operation(summary = "Investor: Gửi thông số đầu vào để AI phân bổ -> Tạo mới 1 bản CHA (InvestmentProfile) và 1 bản CON (InvestmentProfileVersion)")
    public ResponseEntity<ApiResponse> generateInvestmentPlan(@RequestBody InvestmentPlanRequest request) {
        return investmentPlanServiceInterface.generateCompleteInvestmentPlan(request);
    }

    @PostMapping("/{profileId}")
    @PreAuthorize("hasAnyRole('Investor')")
    @Operation(summary = "Investor: Thay đổi con số tính toán -> Giữ nguyên CHA (InvestmentProfile) cũ nhưng đẻ thêm 1 bản CON (InvestmentProfileVersion) mới tinh để lưu lịch sử")
    public ResponseEntity<ApiResponse> updateInvestmentPlan(
            @PathVariable Integer profileId,
            @RequestBody UpdateInvestmentPlanRequest request) {
        return investmentPlanServiceInterface.updateExistingInvestmentPlan(profileId, request);
    }

    @DeleteMapping("/{versionId}")
    @PreAuthorize("hasAnyRole('Investor')")
    @Operation(summary = "Investor: Xóa 1 bản CON (InvestmentProfileVersion) cụ thể ra khỏi danh sách lịch sử")
    public ResponseEntity<ApiResponse> deleteInvestmentProfileVersion(@PathVariable Integer versionId){
        return investmentPlanServiceInterface.deleteInvestmentPlanVersion(versionId);
    }

//    @PostMapping("/save")
//    @PreAuthorize("hasAnyRole('Investor')")
//    public ResponseEntity<ApiResponse> saveInvestmentPlan(@RequestBody SaveInvestmentPlanRequest request) {
//        return investmentPlanServiceInterface.savePlanToDatabaseDirectly(request);
//    }
}