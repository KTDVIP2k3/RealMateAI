package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateAccountRequestV2;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

public interface AccountServiceInterface {
    ResponseEntity<ApiResponse> getAccountProfile();

    ResponseEntity<ApiResponse> createAccountByAdmin(CreateAccountRequest createAccountRequest);

    public ResponseEntity<ApiResponse> createAccountAdmin(CreateAccountRequest createAccountRequest);

    ResponseEntity<ApiResponse> createAccount(CreateAccountRequestV2 createAccountRequestV2);
}
