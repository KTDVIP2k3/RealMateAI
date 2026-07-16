package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.AccountVerificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AccountVerificationUpdateRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface AccountVerificationServiceInterface {

    ResponseEntity<ApiResponse> getAccountVerificationByStaffOrAdmin();

     ResponseEntity<ApiResponse> getAccountVerificationByIdByStaffOrAdmin(Integer id);

    ResponseEntity<ApiResponse> getAccountVerificationForUser();

    ResponseEntity<ApiResponse> getAccountVerificationDetailForUser(Integer id);

    ResponseEntity<ApiResponse> createAccountVerification(AccountVerificationRequest request);

    ResponseEntity<ApiResponse> updateAccountVerification(AccountVerificationUpdateRequest request);

    ResponseEntity<ApiResponse> approveAccountVerification(Integer id);

    ResponseEntity<ApiResponse> rejectAccountVerification(Integer id, String reason);
}
