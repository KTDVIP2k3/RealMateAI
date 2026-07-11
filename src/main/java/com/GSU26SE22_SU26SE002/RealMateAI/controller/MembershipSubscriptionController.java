package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.MembershipSubscriptionServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/membership-subscriptions")
@Tag(name = "Membership Subscription", description = "Investor: Quản lý và đăng ký gói thành viên")
public class MembershipSubscriptionController {

    @Autowired
    private MembershipSubscriptionServiceInterface membershipSubscriptionServiceInterface;

    @GetMapping
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Lấy danh sách các gói thành viên đã đăng ký của cá nhân")
    public ResponseEntity<ApiResponse> getMembershipSubscriptions() {
        return membershipSubscriptionServiceInterface.getMembershipSubscriptions();
    }

    @PostMapping("/pay")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Thanh toán mua mới một gói thành viên bằng số dư ví")
    public ResponseEntity<ApiResponse> payMemberShipSubscriptions(@RequestParam Integer membershipPlanId) {
        return membershipSubscriptionServiceInterface.payMemberShipSubscriptions(membershipPlanId);
    }

    @PutMapping("/{id}/renew")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Gia hạn gói thành viên hiện tại bằng số dư ví")
    public ResponseEntity<ApiResponse> renewMemberShipSubscriptions(@PathVariable("id") Integer membershipSubscriptionId) {
        return membershipSubscriptionServiceInterface.renewMemberShipSubscriptions(membershipSubscriptionId);
    }
}