package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.MembershipPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.MembershipPlanServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@Tag(name = "Membership-Plan")
public class MembershipPlanController {

    @Autowired
    private MembershipPlanServiceInterface membershipPlanServiceInterface;

    @GetMapping(value = "/membership-plans/active", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = " Lấy danh sách các gói thành viên ĐANG HOẠT ĐỘNG")
    public ResponseEntity<ApiResponse> getMembershipPlanListIsActive() {
        return membershipPlanServiceInterface.getMembershipPlanListIsActive();
    }

    @GetMapping(value = "/membership-plans/{membershipPlanId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = " Xem chi tiết một gói thành viên theo ID")
    public ResponseEntity<ApiResponse> getMembershipPlanDetail(@PathVariable("membershipPlanId") Integer id) {
        return membershipPlanServiceInterface.getMembershipPlanDetail(id);
    }

    @GetMapping(value = "/admin/membership-plans", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: Lấy TOÀN BỘ danh sách gói thành viên")
    public ResponseEntity<ApiResponse> getMembershipPlanListByAdmin() {
        return membershipPlanServiceInterface.getMembershipPlanListByAdmin();
    }

    @PostMapping(value = "/admin/membership-plans")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: Tạo MỚI một gói thành viên")
    public ResponseEntity<ApiResponse> createMembershipPlan(@RequestBody MembershipPlanRequest membershipPlanRequest) {
        return membershipPlanServiceInterface.createMembershipPlan(membershipPlanRequest);
    }

    @PutMapping(value = "/admin/membership-plans/{membershipPlanId}")
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: CẬP NHẬT thông tin gói thành viên")
    public ResponseEntity<ApiResponse> updateMembershipPlan(@PathVariable("membershipPlanId") Integer id, @RequestBody MembershipPlanRequest membershipPlanRequest) {
        return membershipPlanServiceInterface.updateMembershipPlan(id, membershipPlanRequest);
    }

    @DeleteMapping(value = "/admin/membership-plans/{membershipPlanId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: XÓA MỀM gói thành viên (Ẩn gói)")
    public ResponseEntity<ApiResponse> deleteMembershipPlan(@PathVariable("membershipPlanId") Integer id) {
        return membershipPlanServiceInterface.deleteMembershipPlan(id);
    }

    @PatchMapping(value = "/admin/membership-plans/{membershipPlanId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('Admin')")
    @Operation(summary = "Admin: TẠM NGƯNG hoặc TÁI KÍCH HOẠT gói thành viên")
    public ResponseEntity<ApiResponse> toggleActiveMembershipPlan(
            @PathVariable("membershipPlanId") Integer id,
            @RequestParam Boolean isActive) {
        return membershipPlanServiceInterface.toggleActiveMembershipPlan(id, isActive);
    }
}