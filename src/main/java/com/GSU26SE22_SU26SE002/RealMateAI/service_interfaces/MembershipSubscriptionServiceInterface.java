package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface MembershipSubscriptionServiceInterface {
    ResponseEntity<ApiResponse> getMembershipSubscriptions(int page, int size);
    ResponseEntity<ApiResponse> payMemberShipSubscriptions(Integer membershipPlanId);
    ResponseEntity<ApiResponse> renewMemberShipSubscriptions(Integer membershipSubscriptionId);
    ResponseEntity<ApiResponse> activeMembershipSubscriptions(Integer membershipPlanId);
}
