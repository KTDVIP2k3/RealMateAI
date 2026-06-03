package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.AccountProfileDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AccountServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AccountServiceImplement implements AccountServiceInterface {
    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AuthenUntil authenUntil;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public ResponseEntity<ApiResponse> getAccountProfile() {
        try{
            if(authenUntil.getCurrentUSer() == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "Account does not exist"));
            }
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(modelMapper.map(authenUntil.getCurrentUSer(), AccountProfileDTO.class), "Account Profile"));
        }catch (Exception e){
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}
