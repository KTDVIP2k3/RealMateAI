package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.MembershipPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface MembershipPlanServiceInterface {
    ResponseEntity<ApiResponse> getMembershipPlanListIsActive();
    ResponseEntity<ApiResponse> getMembershipPlanListByAdmin();
    ResponseEntity<ApiResponse> getMembershipPlanDetail(Integer id);
    ResponseEntity<ApiResponse> createMembershipPlan(MembershipPlanRequest membershipPlanRequest);
    ResponseEntity<ApiResponse> updateMembershipPlan(Integer id, MembershipPlanRequest membershipPlanRequest);
    ResponseEntity<ApiResponse> deleteMembershipPlan(Integer id);
}