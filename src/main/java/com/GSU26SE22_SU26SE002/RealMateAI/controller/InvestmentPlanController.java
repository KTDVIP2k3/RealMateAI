package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.NameUpdateRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SaveInvestmentPlanRequest;
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
@RequestMapping("/investment-plans")
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
    public ResponseEntity<ApiResponse> getProfilesByInvest(Integer profileId){
        return investmentPlanServiceInterface.getListViewsByProfileId(profileId);
    }

    @GetMapping("/versions/{versionId}/inputs")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Xem chi tiết thông số cài đặt đầu vào của CON (InvestmentProfileVersion)")
    public ResponseEntity<ApiResponse> getProfileDetailById(@PathVariable Integer versionId) {
        return investmentPlanServiceInterface.getProfileVersionDetailById(versionId);
    }

    @GetMapping("/versions/{versionId}/results")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Xem chi tiết kết quả dòng tiền và giỏ hàng của CON (InvestmentProfileVersion)")
    public ResponseEntity<ApiResponse> getInvestmentPlanDetailByProfileId(@PathVariable Integer versionId) {
        return investmentPlanServiceInterface.getInvestmentPlanDetailByVersionId(versionId);
    }

    @PostMapping
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Gửi thông số đầu vào để AI phân bổ -> Tạo mới 1 bản CHA (InvestmentProfile) và 1 bản CON (InvestmentProfileVersion)")
    public ResponseEntity<ApiResponse> generateInvestmentPlan(@RequestBody InvestmentPlanRequest request) {
        return investmentPlanServiceInterface.generateCompleteInvestmentPlan(request);
    }

    @PostMapping("/{Id}")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Thay đổi con số tính toán -> Giữ nguyên CHA (InvestmentProfile) cũ nhưng đẻ thêm 1 bản CON (InvestmentProfileVersion) mới tinh để lưu lịch sử")
    public ResponseEntity<ApiResponse> updateInvestmentPlan(
            @PathVariable Integer Id,
            @RequestBody UpdateInvestmentPlanRequest request) {
        return investmentPlanServiceInterface.updateExistingInvestmentPlan(Id, request);
    }

    @DeleteMapping("/version/{Id}")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Xóa 1 bản CON (InvestmentProfileVersion) cụ thể ra khỏi danh sách lịch sử")
    public ResponseEntity<ApiResponse> deleteInvestmentProfileVersion(@PathVariable Integer Id){
        return investmentPlanServiceInterface.deleteInvestmentPlanVersion(Id);
    }

    @DeleteMapping("/{Id}")
    @Operation(summary = "Investor: Xóa mềm thực thể CHA (InvestmentProfile) và tất cả các phiên bản con liên quan")
    @PreAuthorize("hasRole('Investor')")
    public ResponseEntity<ApiResponse> deleteInvestmentProfile(@PathVariable Integer Id){
        return investmentPlanServiceInterface.deleteInvestmentPlan(Id);
    }

    @PutMapping("/{Id}/name")
    @Operation(summary = "Investor: Cập nhật duy nhất trường Name cho thực thể CHA (InvestmentProfile)")
    @PreAuthorize("hasRole('Investor')")
    public ResponseEntity<ApiResponse> updateProfileName(
            @PathVariable("Id") Integer id,
            @RequestBody NameUpdateRequest request) {
        return investmentPlanServiceInterface.updateProfileName(id, request.getName());
    }

    @PutMapping("/versions/{Id}/name")
    @Operation(summary = "Investor: Cập nhật duy nhất trường Name cho thực thể CON (InvestmentProfileVersion)")
    @PreAuthorize("hasRole('Investor')")
    public ResponseEntity<ApiResponse> updateVersionName(
            @PathVariable("Id") Integer id,
            @RequestBody NameUpdateRequest request) {
        return investmentPlanServiceInterface.updateVersionName(id, request.getName());
    }

//    @PostMapping("/save")
//    @PreAuthorize("hasAnyRole('Investor')")
//    public ResponseEntity<ApiResponse> saveInvestmentPlan(@RequestBody SaveInvestmentPlanRequest request) {
//        return investmentPlanServiceInterface.savePlanToDatabaseDirectly(request);
//    }
}