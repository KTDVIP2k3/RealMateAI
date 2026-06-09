package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.OTP;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.OtpRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.LoginRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.RegisterRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AuthServiceInterface;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthServiceImplement implements AuthServiceInterface {
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    OtpRepository otpRepository;
    @Autowired
    EmailServiceVerificationImplement emailServiceVerificationImplement;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    JwtServiceImplement jwtServiceImplement;

    public ResponseEntity<ApiResponse> resendOtpUnified(HttpSession httpSession) {
        try {
            Integer accountId = (Integer) httpSession.getAttribute("accountId");
            if (accountId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Invalid session"));
            }

            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Account does not exist"));
            }

            emailServiceVerificationImplement.sendVerificationEmail(account);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "A new OTP has been sent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    public ResponseEntity<ApiResponse> sendOtp(HttpSession httpSession) {
        try {
            Integer accountId = (Integer) httpSession.getAttribute("accountId");
            if (accountId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Account information not found"));
            }
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Account does not exist"));
            }
            emailServiceVerificationImplement.sendVerificationEmail(account);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "OTP has been sent to your email."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> register(RegisterRequest registerRequest, HttpSession session) {
        try {
            if (registerRequest.getPhone().isEmpty() || registerRequest.getEmail().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Information cannot be empty"));
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

            accountRepository.saveAndFlush(account);
            int savedAccountId = account.getAccountId();
            session.setAttribute("accountId", savedAccountId);
            emailServiceVerificationImplement.sendVerificationEmail(account);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Information valid. Please enter the OTP sent via Email to complete."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("SERVER_ERROR", "System error: " + e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse> verifyRegister(String otp, HttpSession httpSession) {
        try {
            Integer accountId = (Integer) httpSession.getAttribute("accountId");
            if (accountId == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Invalid session"));
            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Account does not exist"));

            OTP otpEntity = account.getOtp();
            if (otpEntity == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "OTP not found"));
            if (otpEntity.getExpiredAt().isBefore(LocalDateTime.now())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "OTP has expired"));
            if (!otpEntity.getCode().equals(otp)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Incorrect OTP"));

            account.setIsActive(true);
            account.setCreateAt(LocalDateTime.now());
            accountRepository.save(account);
            otpRepository.delete(otpEntity);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Account registration successful!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("SERVER_ERROR", e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse> verifyLogin(String otp, HttpSession httpSession) {
        try {
            Integer accountId = (Integer) httpSession.getAttribute("accountId");
            if (accountId == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Invalid session"));

            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Account does not exist"));

            OTP otpEntity = account.getOtp();
            if (otpEntity == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "OTP not found"));
            if (otpEntity.getExpiredAt().isBefore(LocalDateTime.now())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "OTP has expired"));
            if (!otpEntity.getCode().equals(otp)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Incorrect OTP"));

            String jwt = jwtServiceImplement.generateToken(account.getUsername(), account.getRole().name(), account.getEmail());
            otpRepository.delete(otpEntity);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(jwt, "Login successful"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("SERVER_ERROR", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> login(LoginRequest loginRequest, HttpSession httpSession) {
        try {
            if (loginRequest.getUserName().isEmpty() || loginRequest.getPassword().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "UserName/Password should not be blank"));
            }

            Account account = accountRepository.findByUserName(loginRequest.getUserName()).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "Account does not exist"));
            }

            if (!account.getIsActive()) {
                emailServiceVerificationImplement.sendVerificationEmail(account);
                httpSession.setAttribute("accountId", account.getAccountId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("Forbidden", "Account not activated. OTP sent to email."));
            }

            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUserName(), loginRequest.getPassword()));
            emailServiceVerificationImplement.sendVerificationEmail(account);
            httpSession.setAttribute("accountId", account.getAccountId());

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Password correct. Please check your email for the OTP to complete login."));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Password does not match"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse> activateAccount(String otp, HttpSession httpSession) {
        try {
            Integer accountId = (Integer) httpSession.getAttribute("accountId");
            if (accountId == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Invalid session"));

            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Account does not exist"));

            OTP otpEntity = account.getOtp();
            if (otpEntity == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "OTP not found"));
            if (otpEntity.getExpiredAt().isBefore(LocalDateTime.now())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "OTP has expired"));
            if (!otpEntity.getCode().equals(otp)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Incorrect OTP"));

            account.setIsActive(true);
            accountRepository.save(account);
            otpRepository.delete(otpEntity);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Account activated successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("SERVER_ERROR", e.getMessage()));
        }
    }
}