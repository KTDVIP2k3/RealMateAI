package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.RegisterRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AuthServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImplement implements AuthServiceInterface {
    @Autowired
    AccountRepository accountRepository;


    @Override
    public ResponseEntity<ApiResponse> register(RegisterRequest registerRequest) {
        try{
            if(registerRequest.getPhone().isEmpty()){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "phone khong duoc de trong"));
            }
            Account account = new Account();
            account.setUserName(registerRequest.getUserName());
            account.setPassword(new BCryptPasswordEncoder(12).encode(registerRequest.getPassword()));
            account.setFull_name(registerRequest.getFullName());
            account.setPhone(registerRequest.getPhone());
            account.setEmail(registerRequest.getEmail());
            account.setBirth_date(registerRequest.getBirthDate());
            account.setGender(registerRequest.getGender());
            account.setRole(registerRequest.getRole());
            account.setIsActive(true);
            account.setCreateAt(LocalDateTime.now());
            accountRepository.save(account);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success("Dang ky tai khoan thanh cong!!!"));
        }catch (Exception e){
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}
