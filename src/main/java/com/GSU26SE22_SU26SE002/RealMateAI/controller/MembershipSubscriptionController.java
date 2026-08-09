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
    public ResponseEntity<ApiResponse> getMembershipSubscriptions(@RequestParam(name = "page", required = false, defaultValue = "0") int page,
                                                                  @RequestParam(name = "size", required = false, defaultValue = "10") int size)
    {
        return membershipSubscriptionServiceInterface.getMembershipSubscriptions(page, size);
    }

    @PostMapping
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Thanh toán mua mới một gói thành viên bằng số dư ví")
    public ResponseEntity<ApiResponse> payMemberShipSubscriptions(@RequestParam Integer membershipPlanId) {
        return membershipSubscriptionServiceInterface.payMemberShipSubscriptions(membershipPlanId);
    }

    @PostMapping("/{subscriptionId}/renew")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Gia hạn gói thành viên hiện tại bằng số dư ví")
    public ResponseEntity<ApiResponse> renewMemberShipSubscriptions(@PathVariable("subscriptionId") Integer subscriptionId) {
        return membershipSubscriptionServiceInterface.renewMemberShipSubscriptions(subscriptionId);
    }

    @PutMapping("/{memberSubscriptionId}/active")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Kích hoạt sử dụng  gói thành viên đã mua sang trạng thái đang sử dụng")
    public ResponseEntity<ApiResponse> activeMembershipSubscriptions(@PathVariable("memberSubscriptionId") Integer memberSubscriptionId) {
        return membershipSubscriptionServiceInterface.activeMembershipSubscriptions(memberSubscriptionId);
    }

    @PutMapping("/{memberSubscriptionId}/cancel")
    @PreAuthorize("hasRole('Investor')")
    @Operation(summary = "Investor: Hủy gói thành viên đang sử dụng chuyển sang trạng thái chờ kích hoạt")
    public ResponseEntity<ApiResponse> cancelMembershipSubscriptions(@PathVariable("memberSubscriptionId") Integer memberSubscriptionId) {
        return membershipSubscriptionServiceInterface.cancelMembershipSubscriptions(memberSubscriptionId);
    }
}