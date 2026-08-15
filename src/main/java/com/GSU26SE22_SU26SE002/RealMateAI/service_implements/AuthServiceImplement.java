package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.OTP;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Seller;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.OtpRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.SellerRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AuthServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.mail.MessagingException;
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
    private SellerRepository sellerRepository;
    @Autowired
    private OtpRepository otpRepository;
    @Autowired
    private  EmailServiceVerificationImplement emailServiceVerificationImplement;
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private JwtServiceImplement jwtServiceImplement;
    @Autowired
    private AuthenUntil authenUntil;

    public ResponseEntity<ApiResponse> resendOtpUnified(HttpSession httpSession, SendOtpRequest sendOtpRequest) {
        try {
//            Integer accountId = (Integer) httpSession.getAttribute("accountId");
//            if (accountId == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Invalid session"));
//            }

            Account account = accountRepository.findByEmail(sendOtpRequest.getEmail()).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("RESOURCE_NOT_FOUND", "Email does not exist"));
            }

            emailServiceVerificationImplement.sendVerificationEmail(account);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "A new OTP has been sent successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("SERVER_ERROR", e.getMessage()));
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

            if (registerRequest.getPhone().isEmpty() || registerRequest.getEmail().isEmpty() || registerRequest.getUserName().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Thông tin không được để trống"));
            }

            String username = registerRequest.getUserName();

            if (username.contains(" ") || username.matches(".*\\s.*")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Tên đăng nhập không được chứa khoảng trắng"));
            }

            String validPattern = "^[a-zA-Z0-9._]+$";
            if (!username.matches(validPattern)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Tên đăng nhập không được chứa dấu tiếng Việt hoặc ký tự đặc biệt"));
            }

            if (username.length() < 3 || username.length() > 20) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Tên đăng nhập phải từ 3 đến 20 ký tự"));
            }

            String password = registerRequest.getPassword();

            if (password.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Mật khẩu không được để trống"));
            }

            if (password.contains(" ") || password.matches(".*\\s.*")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Mật khẩu không được chứa khoảng trắng"));
            }

            if (password.length() < 8 || password.length() > 32) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Mật khẩu phải từ 8 đến 32 ký tự"));
            }

            String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$";
            if (!password.matches(passwordPattern)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Mật khẩu phải bao gồm cả chữ hoa, chữ thường, số và ký tự đặc biệt"));
            }

            String email = registerRequest.getEmail();
            RoleEnum role = registerRequest.getRole();

            boolean existEmailWithRole = accountRepository.findAll().stream()
                    .anyMatch(account -> account.getEmail().equalsIgnoreCase(email) && account.getRole() == role);

            if (existEmailWithRole) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Email này đã được đăng ký cho vai trò tương ứng"));
            }

            Account accountExistByName = accountRepository.findByUserName(registerRequest.getUserName()).orElse(null);
            boolean existByName = accountRepository.findAll().stream().anyMatch(account -> account.getUsername().trim().toLowerCase().equalsIgnoreCase(registerRequest.getUserName().trim().toLowerCase()));
            if(existByName){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "UserName: " + registerRequest.getUserName() + " is existed"));
            }
            Account account = new Account();
            account.setUserName(registerRequest.getUserName().toLowerCase());
            account.setPassword(new BCryptPasswordEncoder(12).encode(registerRequest.getPassword()));
            account.setFull_name(registerRequest.getFullName());
            account.setPhone(registerRequest.getPhone());
            account.setEmail(registerRequest.getEmail());
            account.setBirth_date(registerRequest.getBirthDate());
            account.setGender(registerRequest.getGender());
            account.setRole(registerRequest.getRole());

            Account savedAccount = accountRepository.saveAndFlush(account);
            if (registerRequest.getRole() == RoleEnum.Seller) {
                Seller seller = Seller.builder()
                        .account(savedAccount)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                sellerRepository.save(seller);
            }
            int savedAccountId = account.getAccountId();
//            session.setAttribute("accountId", savedAccountId);
            emailServiceVerificationImplement.sendVerificationEmail(account);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Information valid. Please enter the OTP sent via Email to complete."));
        } catch (MessagingException messagingException){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("SERVER_ERROR", "System error: " + messagingException.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("SERVER_ERROR", "System error: " + e.getMessage()));
        }
    }

    public ResponseEntity<ApiResponse> forgotPassword(ForgotPasswordRequest forgotPasswordRequest, HttpSession httpSession){
        try{
            Account account = accountRepository.findByEmail(forgotPasswordRequest.getEmail()).orElse(null);
            boolean existEmail = accountRepository.findAll().stream().anyMatch(account1 -> account1.getEmail().toLowerCase().equalsIgnoreCase(forgotPasswordRequest.getEmail().toLowerCase()));

            if(!existEmail){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_found", "Email: " + forgotPasswordRequest.getEmail() + " does not exist"));
            }

//            httpSession.setAttribute("accountId", account.getAccountId());
            emailServiceVerificationImplement.sendVerificationEmail(account);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Success" + "\n" + "OTP will sent from" + forgotPasswordRequest.getEmail() + " to verify"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }

    }

    @Override
    public ResponseEntity<ApiResponse> resetPassword(ResetPasswordRequest resetPasswordRequest, HttpSession httpSession) {
        try{
            Account account = authenUntil.getCurrentUSer();

            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "User session missing or token expired"));
            }

            String inputUsername = resetPasswordRequest.getUserName().trim().toLowerCase();
            String currentUsername = account.getUsername().toLowerCase();

            if (!currentUsername.equals(inputUsername)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Tên đăng nhập không trùng khớp với tài khoản đang đăng nhập"));
            }

            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


            if (!passwordEncoder.matches(resetPasswordRequest.getOldPassword(), account.getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Bad_Request", "Old password is wrong"));
            }
            account.setPassword(new BCryptPasswordEncoder(12).encode(resetPasswordRequest.getNewPassword()));
            account.setUpdateAt(LocalDateTime.now());
            accountRepository.save(account);
            httpSession.removeAttribute("accountId");

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Password reset successfully! You can now log in with new password."));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Sever_Error", e.getMessage()));
        }

    }

    @Override
    public ResponseEntity<ApiResponse> newPassword(NewPasswordRequest newPasswordRequest, HttpSession httpSession) {
        try {
//            Integer accountId = (Integer) httpSession.getAttribute("accountId");
            Account account = accountRepository.findByEmail(newPasswordRequest.getEmail()).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "Email does not exists"));
            }
            account.setPassword(new BCryptPasswordEncoder(12).encode(newPasswordRequest.getNewPassword()));
            accountRepository.save(account);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Change password successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }


    @Transactional
    public ResponseEntity<ApiResponse> verifyOtp(OtpRequest otpRequest, HttpSession httpSession) {
        try {
//            Integer accountId = (Integer) httpSession.getAttribute("accountId");
            Boolean existAccount = accountRepository.findAll().stream()
                    .anyMatch(a -> a.getEmail().trim().toLowerCase().
                            equalsIgnoreCase(otpRequest.getEmail().trim().toLowerCase()));

            Account account =accountRepository.findAll().stream()
                    .filter(a -> a.getEmail().trim().toLowerCase()
                            .equalsIgnoreCase(otpRequest.getEmail().trim().toLowerCase()))
                    .findFirst()
                    .orElse(null);
            if (account == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Email does not exist"));

            OTP otpEntity = account.getOtp();
            if (otpEntity == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "OTP not found"));
            if (otpEntity.getExpiredAt().isBefore(LocalDateTime.now())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "OTP has expired"));
            if (!otpEntity.getCode().equals(otpRequest.getOtp())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Incorrect OTP"));

            account.setIsActive(true);
            account.setCreateAt(LocalDateTime.now());
            accountRepository.save(account);
            otpRepository.delete(otpEntity);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Verify otp successful!"));
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
//            emailServiceVerificationImplement.sendVerificationEmail(account);
//            httpSession.setAttribute("accountId", account.getAccountId());

            String jwt = jwtServiceImplement.generateToken(account.getUsername(), account.getRole().name(), account.getEmail());

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(jwt, "Login successful"));
        }catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", e.getMessage()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Password does not match"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse> activateAccount(OtpRequest otpRequest, HttpSession httpSession) {
        try {
            Integer accountId = (Integer) httpSession.getAttribute("accountId");
            if (accountId == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Invalid session"));

            Account account = accountRepository.findById(accountId).orElse(null);
            if (account == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Account does not exist"));

            OTP otpEntity = account.getOtp();
            if (otpEntity == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "OTP not found"));
            if (otpEntity.getExpiredAt().isBefore(LocalDateTime.now())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "OTP has expired"));
            if (!otpEntity.getCode().equals(otpRequest.getOtp())) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "Incorrect OTP"));

            account.setIsActive(true);
            accountRepository.save(account);
            otpRepository.delete(otpEntity);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Account activated successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("SERVER_ERROR", e.getMessage()));
        }
    }
}