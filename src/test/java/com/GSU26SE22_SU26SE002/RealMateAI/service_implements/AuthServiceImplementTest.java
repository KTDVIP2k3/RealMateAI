package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.OtpRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.SellerRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.model.OTP;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImplement â€” Authentication")
class AuthServiceImplementTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private OtpRepository otpRepository;
    @Mock
    private EmailServiceVerificationImplement emailServiceVerificationImplement;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtServiceImplement jwtServiceImplement;
    @Mock
    private AuthenUntil authenUntil;
    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private AuthServiceImplement authService;

    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setAccountId(1);
        sampleAccount.setUserName("testuser");
        sampleAccount.setPassword("hashedpassword");
        sampleAccount.setEmail("test@gmail.com");
        sampleAccount.setRole(RoleEnum.Seller);
        sampleAccount.setIsActive(true);
    }

    @Nested
    @DisplayName("F-001. login")
    class LoginTests {

        @Test
        @DisplayName("UC-001: Blank/Empty username returns BAD_REQUEST")
        void login_blankUsername_returnsBadRequest() {
            LoginRequest request = new LoginRequest();
            request.setUserName("");
            request.setPassword("password");

            ResponseEntity<ApiResponse> response = authService.login(request, httpSession);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("UserName/Password should not be blank", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-002: Blank/Empty password returns BAD_REQUEST")
        void login_blankPassword_returnsBadRequest() {
            LoginRequest request = new LoginRequest();
            request.setUserName("testuser");
            request.setPassword("");

            ResponseEntity<ApiResponse> response = authService.login(request, httpSession);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("UserName/Password should not be blank", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-003: Banned Account / Not Active returns FORBIDDEN")
        void login_inactiveAccount_returnsForbidden() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setUserName("testuser");
            request.setPassword("password");

            sampleAccount.setIsActive(false);
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));
            doNothing().when(emailServiceVerificationImplement).sendVerificationEmail(any(Account.class));

            ResponseEntity<ApiResponse> response = authService.login(request, httpSession);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Account not activated.", response.getBody().getMessage());
            verify(emailServiceVerificationImplement).sendVerificationEmail(sampleAccount);
            verify(httpSession).setAttribute("accountId", 1);
        }

        @Test
        @DisplayName("UC-004: Server Error / Exception returns INTERNAL_SERVER_ERROR")
        void login_exceptionThrown_returnsInternalServerError() {
            LoginRequest request = new LoginRequest();
            request.setUserName("testuser");
            request.setPassword("password");

            when(accountRepository.findAll()).thenThrow(new RuntimeException("Database error"));

            ResponseEntity<ApiResponse> response = authService.login(request, httpSession);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Database error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-005: Incorrect password returns BAD_REQUEST")
        void login_incorrectPassword_returnsBadRequest() {
            LoginRequest request = new LoginRequest();
            request.setUserName("testuser");
            request.setPassword("wrongpassword");

            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            ResponseEntity<ApiResponse> response = authService.login(request, httpSession);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Password does not match", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-006: Non-existent username returns NOT_FOUND")
        void login_nonExistentUsername_returnsNotFound() {
            LoginRequest request = new LoginRequest();
            request.setUserName("unknownuser");
            request.setPassword("password");

            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));

            ResponseEntity<ApiResponse> response = authService.login(request, httpSession);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-007: Correct username and password returns OK")
        void login_success_returnsOk() {
            LoginRequest request = new LoginRequest();
            request.setUserName("testuser");
            request.setPassword("password");

            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));
            when(jwtServiceImplement.generateToken(anyString(), anyString(), anyString())).thenReturn("mock-jwt-token");

            ResponseEntity<ApiResponse> response = authService.login(request, httpSession);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Login successful", response.getBody().getMessage());
            assertEquals("mock-jwt-token", response.getBody().getData());
        }
    }

    @Nested
    @DisplayName("F-002. register")
    class RegisterTests {

        private RegisterRequest validRequest;

        @BeforeEach
        void setupRegister() {
            validRequest = new RegisterRequest();
            validRequest.setPhone("0123456789");
            validRequest.setEmail("newuser@gmail.com");
            validRequest.setUserName("newuser");
            validRequest.setPassword("Valid1@Password");
            validRequest.setFullName("New User");
            validRequest.setRole(RoleEnum.Seller);
        }

        @Test
        @DisplayName("UC-008, 009, 010: Blank phone, email, or username returns BAD_REQUEST")
        void register_blankFields_returnsBadRequest() {
            validRequest.setPhone("");
            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("ThÃ´ng tin khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-011: Username contains spaces returns BAD_REQUEST")
        void register_usernameWithSpaces_returnsBadRequest() {
            validRequest.setUserName("new user");
            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("TÃªn Ä‘Äƒng nháº­p khÃ´ng Ä‘Æ°á»£c chá»©a khoáº£ng tráº¯ng", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-012: Username contains special chars returns BAD_REQUEST")
        void register_usernameSpecialChars_returnsBadRequest() {
            validRequest.setUserName("newuser@!");
            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("TÃªn Ä‘Äƒng nháº­p khÃ´ng Ä‘Æ°á»£c chá»©a dáº¥u tiáº¿ng Viá»‡t hoáº·c kÃ½ tá»± Ä‘áº·c biá»‡t", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-013: Username length < 3 or > 20 returns BAD_REQUEST")
        void register_usernameInvalidLength_returnsBadRequest() {
            validRequest.setUserName("ab");
            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("TÃªn Ä‘Äƒng nháº­p pháº£i tá»« 3 Ä‘áº¿n 20 kÃ½ tá»±", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-014: Password blank returns BAD_REQUEST")
        void register_blankPassword_returnsBadRequest() {
            validRequest.setPassword("");
            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Máº­t kháº©u khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-015: Password contains spaces returns BAD_REQUEST")
        void register_passwordWithSpaces_returnsBadRequest() {
            validRequest.setPassword("Valid 1@");
            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Máº­t kháº©u khÃ´ng Ä‘Æ°á»£c chá»©a khoáº£ng tráº¯ng", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-016: Password length < 8 or > 32 returns BAD_REQUEST")
        void register_passwordInvalidLength_returnsBadRequest() {
            validRequest.setPassword("Val1@");
            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Máº­t kháº©u pháº£i tá»« 8 Ä‘áº¿n 32 kÃ½ tá»±", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-017: Password invalid format returns BAD_REQUEST")
        void register_passwordInvalidFormat_returnsBadRequest() {
            validRequest.setPassword("invalidpassword123");
            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Máº­t kháº©u pháº£i bao gá»“m cáº£ chá»¯ hoa, chá»¯ thÆ°á»ng, sá»‘ vÃ  kÃ½ tá»± Ä‘áº·c biá»‡t", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-018: Existed email with same role returns BAD_REQUEST")
        void register_existedEmailSameRole_returnsBadRequest() {
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));
            validRequest.setEmail("test@gmail.com"); // sampleAccount's email
            validRequest.setRole(RoleEnum.Seller); // sampleAccount's role
            
            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Email nÃ y Ä‘Ã£ Ä‘Æ°á»£c Ä‘Äƒng kÃ½ cho vai trÃ² tÆ°Æ¡ng á»©ng", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-019: Existed username returns BAD_REQUEST")
        void register_existedUsername_returnsBadRequest() {
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));
            validRequest.setEmail("different@gmail.com");
            validRequest.setUserName("testuser"); // sampleAccount's username

            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("UserName: testuser is existed", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-022: Valid registration returns OK")
        void register_valid_returnsOk() throws Exception {
            when(accountRepository.findAll()).thenReturn(List.of());
            when(accountRepository.saveAndFlush(any(Account.class))).thenAnswer(i -> {
                Account a = i.getArgument(0);
                a.setAccountId(2);
                return a;
            });

            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Information valid. Please enter the OTP sent via Email to complete.", response.getBody().getMessage());
            verify(emailServiceVerificationImplement).sendVerificationEmail(any(Account.class));
            verify(sellerRepository).save(any());
        }
        
        @Test
        @DisplayName("UC-020: MessagingException returns INTERNAL_SERVER_ERROR")
        void register_messagingException_returnsServerError() throws Exception {
            when(accountRepository.findAll()).thenReturn(List.of());
            when(accountRepository.saveAndFlush(any(Account.class))).thenReturn(new Account());
            doThrow(new MessagingException("Mail error")).when(emailServiceVerificationImplement).sendVerificationEmail(any(Account.class));

            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("System error: Mail error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-021: Generic Exception returns INTERNAL_SERVER_ERROR")
        void register_genericException_returnsServerError() {
            when(accountRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = authService.register(validRequest, httpSession);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("System error: DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("F-003. verify-otp")
    class VerifyOtpTests {

        private OtpRequest request;
        private OTP otp;

        @BeforeEach
        void setupOtp() {
            request = new OtpRequest();
            request.setEmail("test@gmail.com");
            request.setOtp("123456");

            otp = new OTP();
            otp.setCode("123456");
            otp.setExpiredAt(LocalDateTime.now().plusMinutes(5));
            sampleAccount.setOtp(otp);
        }

        @Test
        @DisplayName("UC-023: Valid OTP returns OK")
        void verifyOtp_valid_returnsOk() {
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));

            ResponseEntity<ApiResponse> response = authService.verifyOtp(request, httpSession);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Verify otp successful!", response.getBody().getMessage());
            verify(accountRepository).save(sampleAccount);
            verify(otpRepository).delete(otp);
        }

        @Test
        @DisplayName("UC-024: Exception returns INTERNAL_SERVER_ERROR")
        void verifyOtp_exception_returnsServerError() {
            when(accountRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = authService.verifyOtp(request, httpSession);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-025: Non-existent email returns BAD_REQUEST")
        void verifyOtp_nonExistentEmail_returnsBadRequest() {
            when(accountRepository.findAll()).thenReturn(List.of());

            ResponseEntity<ApiResponse> response = authService.verifyOtp(request, httpSession);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Email does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-026: OTP not found in DB returns BAD_REQUEST")
        void verifyOtp_otpNotFound_returnsBadRequest() {
            sampleAccount.setOtp(null);
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));

            ResponseEntity<ApiResponse> response = authService.verifyOtp(request, httpSession);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("OTP not found", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-027: Expired OTP returns BAD_REQUEST")
        void verifyOtp_expiredOtp_returnsBadRequest() {
            otp.setExpiredAt(LocalDateTime.now().minusMinutes(5));
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));

            ResponseEntity<ApiResponse> response = authService.verifyOtp(request, httpSession);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("OTP has expired", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-028: Incorrect OTP returns BAD_REQUEST")
        void verifyOtp_incorrectOtp_returnsBadRequest() {
            request.setOtp("654321");
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));

            ResponseEntity<ApiResponse> response = authService.verifyOtp(request, httpSession);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Incorrect OTP", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("F-004. send-otp")
    class SendOtpTests {

        @Test
        @DisplayName("UC-030: Valid email returns OK")
        void sendOtp_valid_returnsOk() throws Exception {
            SendOtpRequest request = new SendOtpRequest();
            request.setEmail("test@gmail.com");
            when(accountRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(sampleAccount));

            ResponseEntity<ApiResponse> response = authService.resendOtpUnified(httpSession, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("A new OTP has been sent successfully!", response.getBody().getMessage());
            verify(emailServiceVerificationImplement).sendVerificationEmail(sampleAccount);
        }

        @Test
        @DisplayName("UC-033: Non-existent email returns NOT_FOUND")
        void sendOtp_nonExistentEmail_returnsNotFound() {
            SendOtpRequest request = new SendOtpRequest();
            request.setEmail("unknown@gmail.com");
            when(accountRepository.findByEmail(anyString())).thenReturn(java.util.Optional.empty());

            ResponseEntity<ApiResponse> response = authService.resendOtpUnified(httpSession, request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Email does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-031, UC-032: Exception returns INTERNAL_SERVER_ERROR")
        void sendOtp_exception_returnsServerError() {
            SendOtpRequest request = new SendOtpRequest();
            request.setEmail("test@gmail.com");
            when(accountRepository.findByEmail(anyString())).thenThrow(new RuntimeException("Error"));

            ResponseEntity<ApiResponse> response = authService.resendOtpUnified(httpSession, request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("F-005. forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("UC-034: Existed email returns OK")
        void forgotPassword_valid_returnsOk() throws Exception {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("test@gmail.com");
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));
            when(accountRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(sampleAccount));

            ResponseEntity<ApiResponse> response = authService.forgotPassword(request, httpSession);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Success\nOTP will sent fromtest@gmail.com to verify", response.getBody().getMessage());
            verify(emailServiceVerificationImplement).sendVerificationEmail(sampleAccount);
        }

        @Test
        @DisplayName("UC-036: Non-existent email returns NOT_FOUND")
        void forgotPassword_nonExistentEmail_returnsNotFound() {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("unknown@gmail.com");
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));

            ResponseEntity<ApiResponse> response = authService.forgotPassword(request, httpSession);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Email: unknown@gmail.com does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-035: Exception returns INTERNAL_SERVER_ERROR")
        void forgotPassword_exception_returnsServerError() {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("test@gmail.com");
            when(accountRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = authService.forgotPassword(request, httpSession);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("F-006. create new-password")
    class CreateNewPasswordTests {

        @Test
        @DisplayName("UC-037: Valid new password returns OK")
        void newPassword_valid_returnsOk() {
            NewPasswordRequest request = new NewPasswordRequest();
            request.setEmail("test@gmail.com");
            request.setNewPassword("NewPass1@");
            when(accountRepository.findByEmail(anyString())).thenReturn(java.util.Optional.of(sampleAccount));

            ResponseEntity<ApiResponse> response = authService.newPassword(request, httpSession);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Change password successfully", response.getBody().getMessage());
            verify(accountRepository).save(sampleAccount);
        }

        @Test
        @DisplayName("UC-039: Non-existent email returns NOT_FOUND")
        void newPassword_nonExistentEmail_returnsNotFound() {
            NewPasswordRequest request = new NewPasswordRequest();
            request.setEmail("unknown@gmail.com");
            when(accountRepository.findByEmail(anyString())).thenReturn(java.util.Optional.empty());

            ResponseEntity<ApiResponse> response = authService.newPassword(request, httpSession);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Email does not exists", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-038: Exception returns INTERNAL_SERVER_ERROR")
        void newPassword_exception_returnsServerError() {
            NewPasswordRequest request = new NewPasswordRequest();
            request.setEmail("test@gmail.com");
            when(accountRepository.findByEmail(anyString())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = authService.newPassword(request, httpSession);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("F-007. reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("UC-040: Correct Old Password returns OK")
        void resetPassword_valid_returnsOk() {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setOldPassword("password");
            request.setNewPassword("NewPass1@");

            sampleAccount.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12).encode("password"));
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = authService.resetPassword(request, httpSession);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Password reset successfully! You can now log in with new password.", response.getBody().getMessage());
            verify(accountRepository).save(sampleAccount);
        }

        @Test
        @DisplayName("UC-042: Incorrect Old Password returns BAD_REQUEST")
        void resetPassword_incorrectOldPassword_returnsBadRequest() {
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setOldPassword("wrongpassword");
            request.setNewPassword("NewPass1@");

            sampleAccount.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12).encode("password"));
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = authService.resetPassword(request, httpSession);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Old password is wrong", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-041: Exception returns INTERNAL_SERVER_ERROR")
        void resetPassword_exception_returnsServerError() {
            ResetPasswordRequest request = new ResetPasswordRequest();
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = authService.resetPassword(request, httpSession);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }
}
