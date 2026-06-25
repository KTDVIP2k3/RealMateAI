package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.AdminCreateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AdminUpdateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface AdminAccountServiceInterface {

    ResponseEntity<ApiResponse> getAllAccounts(Pageable pageable, String role, String keyword);

    ResponseEntity<ApiResponse> getAccountById(Integer accountId);

    ResponseEntity<ApiResponse> createInvestorAccount(AdminCreateAccountRequest request);

    ResponseEntity<ApiResponse> createSellerAccount(AdminCreateAccountRequest request);

    ResponseEntity<ApiResponse> updateAccount(Integer accountId, AdminUpdateAccountRequest request);

    ResponseEntity<ApiResponse> changeRole(Integer accountId, String role);

    ResponseEntity<ApiResponse> setAccountStatus(Integer accountId, Boolean isActive);

}
