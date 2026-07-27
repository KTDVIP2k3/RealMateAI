package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.NameUpdateRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestmentPlanServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/investor/investment-plans")
@Tag(name = "Investment Plan", description = "Investor: Quản lý phương án và kế hoạch đầu tư")
public class InvestmentPlanController {

    @Autowired
    private InvestmentPlanServiceInterface investmentPlanServiceInterface;

    @GetMapping("/me")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Lấy danh sách CHA (InvestmentProfile) - thông số hiển thị lấy từ bản CON (InvestmentProfileVersion) mới nhất")
    public ResponseEntity<ApiResponse> getProfilesByInvest(){
        return investmentPlanServiceInterface.getListProfileByInvestor();
    }

    @GetMapping("/{profileId}/versions")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Lấy danh sách các CON (InvestmentProfileVersion) thuộc về CHA (InvestmentProfile) này")
    public ResponseEntity<ApiResponse> getProfilesByInvest(@PathVariable("profileId") Integer profileId){
        return investmentPlanServiceInterface.getListViewsByProfileId(profileId);
    }

    @GetMapping("/versions/{versionId}/inputs")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Xem chi tiết thông số cài đặt đầu vào của CON (InvestmentProfileVersion)")
    public ResponseEntity<ApiResponse> getProfileDetailById(@PathVariable("versionId") Integer versionId) {
        return investmentPlanServiceInterface.getProfileVersionDetailById(versionId);
    }

    @GetMapping("/versions/{versionId}/results")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Xem chi tiết kết quả dòng tiền và giỏ hàng của CON (InvestmentProfileVersion)")
    public ResponseEntity<ApiResponse> getInvestmentPlanDetailByProfileId(@PathVariable("versionId") Integer versionId) {
        return investmentPlanServiceInterface.getInvestmentPlanDetailByVersionId(versionId);
    }

    @PostMapping
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Gửi thông số đầu vào để AI phân bổ -> Tạo mới 1 bản CHA (InvestmentProfile) và 1 bản CON (InvestmentProfileVersion)")
    public ResponseEntity<ApiResponse> generateInvestmentPlan(@RequestBody InvestmentPlanRequest request) {
        return investmentPlanServiceInterface.generateCompleteInvestmentPlan(request);
    }

    @PostMapping("/{profileId}/versions")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Thay đổi con số tính toán -> Giữ nguyên CHA (InvestmentProfile) cũ nhưng đẻ thêm 1 bản CON (InvestmentProfileVersion) mới tinh để lưu lịch sử")
    public ResponseEntity<ApiResponse> updateInvestmentPlan(
            @PathVariable("profileId") Integer profileId,
            @RequestBody UpdateInvestmentPlanRequest request) {
        return investmentPlanServiceInterface.updateExistingInvestmentPlan(profileId, request);
    }

    @DeleteMapping("/investor/investment-plans/versions/{versionId}")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Xóa 1 bản CON (InvestmentProfileVersion) cụ thể ra khỏi danh sách lịch sử")
    public ResponseEntity<ApiResponse> deleteInvestmentProfileVersion(@PathVariable("versionId") Integer versionId){
        return investmentPlanServiceInterface.deleteInvestmentPlanVersion(versionId);
    }

    @DeleteMapping("/{profileId}")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Xóa mềm thực thể CHA (InvestmentProfile) và tất cả các phiên bản con liên quan")
    public ResponseEntity<ApiResponse> deleteInvestmentProfile(@PathVariable("profileId") Integer profileId){
        return investmentPlanServiceInterface.deleteInvestmentPlan(profileId);
    }

    @PatchMapping("/{profileId}")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Cập nhật duy nhất trường Name cho thực thể CHA (InvestmentProfile)")
    public ResponseEntity<ApiResponse> updateProfileName(
            @PathVariable("profileId") Integer profileId,
            @RequestBody NameUpdateRequest request) {
        return investmentPlanServiceInterface.updateProfileName(profileId, request.getName());
    }

    @PatchMapping("/versions/{versionId}")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Cập nhật duy nhất trường Name cho thực thể CON (InvestmentProfileVersion)")
    public ResponseEntity<ApiResponse> updateVersionName(
            @PathVariable("versionId") Integer versionId,
            @RequestBody NameUpdateRequest request) {
        return investmentPlanServiceInterface.updateVersionName(versionId, request.getName());
    }
}