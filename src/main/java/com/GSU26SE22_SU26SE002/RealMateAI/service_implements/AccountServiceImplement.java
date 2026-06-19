package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.AccountProfileDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AccountServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class AccountServiceImplement implements AccountServiceInterface {
    @Autowired
    AccountRepository accountRepository;

    @Autowired
    AuthenUntil authenUntil;

    @Autowired
    ModelMapper modelMapper;

    @Autowired
    EmailServiceVerificationImplement emailServiceVerificationImplement;

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

    @Override
    public ResponseEntity<ApiResponse> createAccountByAdmin(CreateAccountRequest createAccountRequest) {
        try{
            List<Account> accounts = accountRepository.findAll().stream().toList();
            boolean existName = accounts.stream().anyMatch(account -> account.getUsername().equalsIgnoreCase(createAccountRequest.getUserName()));
            boolean existEmail = accounts.stream().anyMatch(account -> account.getEmail().toLowerCase().
                    equalsIgnoreCase(createAccountRequest.getEmail().toLowerCase()));
            if(existName){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "User Name exists"));
            }

            if(existEmail){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Email exists"));
            }

            Account account = new Account();
            account.setUserName(createAccountRequest.getUserName());
            account.setPassword(new BCryptPasswordEncoder(12).encode(createAccountRequest.getPassword()));
            account.setEmail(createAccountRequest.getEmail());
            account.setFull_name(createAccountRequest.getFullName());
            account.setGender(createAccountRequest.getGender());
            account.setPhone(createAccountRequest.getPhone());
            account.setRole(RoleEnum.Staff);
            account.setBirth_date(createAccountRequest.getBirthDate());
            account.setIsActive(true);
            account.setCreateAt(LocalDateTime.now());
            accountRepository.save(account);
            emailServiceVerificationImplement.sendInfoAccountStaff(createAccountRequest.getEmail(), createAccountRequest.getUserName(), createAccountRequest.getPassword());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Create account successfully"));
        }catch (Exception e)    {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createAccountAdmin(CreateAccountRequest createAccountRequest) {
        try{
            List<Account> accounts = accountRepository.findAll().stream().toList();
            boolean existName = accounts.stream().anyMatch(account -> account.getUsername().equalsIgnoreCase(createAccountRequest.getUserName()));
            boolean existEmail = accounts.stream().anyMatch(account -> account.getEmail().toLowerCase().
                    equalsIgnoreCase(createAccountRequest.getEmail().toLowerCase()));
            if(existName){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "User Name exists"));
            }

            if(existEmail){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Email exists"));
            }

            Account account = new Account();
            account.setUserName(createAccountRequest.getUserName());
            account.setPassword(new BCryptPasswordEncoder(12).encode(createAccountRequest.getPassword()));
            account.setEmail(createAccountRequest.getEmail());
            account.setFull_name(createAccountRequest.getFullName());
            account.setGender(createAccountRequest.getGender());
            account.setPhone(createAccountRequest.getPhone());
            account.setRole(RoleEnum.Admin);
            account.setBirth_date(createAccountRequest.getBirthDate());
            account.setIsActive(true);
            account.setCreateAt(LocalDateTime.now());
            accountRepository.save(account);
            emailServiceVerificationImplement.sendInfoAccountStaff(createAccountRequest.getEmail(), createAccountRequest.getUserName(), createAccountRequest.getPassword());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Create account successfully"));
        }catch (Exception e)    {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }


}
