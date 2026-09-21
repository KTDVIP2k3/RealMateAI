package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.InvestorRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.SellerRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AdminCreateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AdminUpdateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NotificationService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAccountServiceImplement - Admin Account Management")
class AdminAccountServiceImplementTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private InvestorRepository investorRepository;
    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EmailServiceVerificationImplement emailService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AdminAccountServiceImplement adminService;

    private Account sampleAccount;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setAccountId(1);
        sampleAccount.setUserName("testuser");
        sampleAccount.setEmail("test@gmail.com");
        sampleAccount.setFull_name("Test User");
        sampleAccount.setPhone("0123456789");
        sampleAccount.setRole(RoleEnum.Seller);
        sampleAccount.setIsActive(true);
        pageable = PageRequest.of(0, 10);
    }

    @Nested
    @DisplayName("View Accounts")
    class ViewAccountsTests {

        @Test
        @DisplayName("Valid role filter returns OK with account list")
        void getAllAccounts_validRole_returnsOk() {
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));

            ResponseEntity<ApiResponse> response = adminService.getAllAccounts(pageable, "Seller", "");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Danh sách tài khoản", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getAllAccounts_exception_returnsServerError() {
            when(accountRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = adminService.getAllAccounts(pageable, null, null);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Invalid role returns BAD_REQUEST")
        void getAllAccounts_invalidRole_returnsBadRequest() {
            when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));

            ResponseEntity<ApiResponse> response = adminService.getAllAccounts(pageable, "INVALID_ROLE", "");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Role không hợp lệ: INVALID_ROLE", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("View Account Details")
    class ViewAccountDetailsTests {

        @Test
        @DisplayName("Existed account ID returns OK")
        void getAccountById_valid_returnsOk() {
            when(accountRepository.findById(1)).thenReturn(Optional.of(sampleAccount));

            ResponseEntity<ApiResponse> response = adminService.getAccountById(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Chi tiết tài khoản", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getAccountById_exception_returnsServerError() {
            when(accountRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = adminService.getAccountById(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent account ID returns NOT_FOUND")
        void getAccountById_notFound_returnsNotFound() {
            when(accountRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = adminService.getAccountById(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Không tìm thấy tài khoản ID: 999", response.getBody().getMessage());
        }
    }


    @Nested
    @DisplayName("Update Account")
    class UpdateAccountTests {

        @Test
        @DisplayName("Valid update returns OK")
        void updateAccount_valid_returnsOk() {
            AdminUpdateAccountRequest request = new AdminUpdateAccountRequest();
            request.setFullName("Updated Name");
            when(accountRepository.findById(1)).thenReturn(Optional.of(sampleAccount));
            when(accountRepository.saveAndFlush(any())).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = adminService.updateAccount(1, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Cập nhật tài khoản thành công", response.getBody().getMessage());
        }

        @ParameterizedTest
        @DisplayName("Blank fields return BAD_REQUEST")
        @CsvSource({
                "'', '0987654321'",
                "'Updated Name', ''"
        })
        void updateAccount_blankFields_returnsBadRequest(String fullName, String phone) {
            AdminUpdateAccountRequest request = new AdminUpdateAccountRequest();
            request.setFullName(fullName);
            request.setPhone(phone);

            ResponseEntity<ApiResponse> response = adminService.updateAccount(1, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Thông tin không được để trống", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void updateAccount_exception_returnsServerError() {
            when(accountRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = adminService.updateAccount(1, new AdminUpdateAccountRequest());

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent account returns NOT_FOUND")
        void updateAccount_notFound_returnsNotFound() {
            when(accountRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = adminService.updateAccount(999, new AdminUpdateAccountRequest());

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertTrue(response.getBody().getMessage().contains("Không tìm thấy tài khoản ID: 999"));
        }
    }

    @Nested
    @DisplayName("Ban Account")
    class BanAccountTests {

        @Test
        @DisplayName("Valid ban (isActive=false) returns OK")
        void setAccountStatus_ban_returnsOk() {
            when(accountRepository.findById(1)).thenReturn(Optional.of(sampleAccount));
            when(accountRepository.save(any())).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = adminService.setAccountStatus(1, false);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Vô hiệu hoá tài khoản thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void setAccountStatus_ban_exception_returnsServerError() {
            when(accountRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = adminService.setAccountStatus(1, false);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent account returns NOT_FOUND")
        void setAccountStatus_ban_notFound_returnsNotFound() {
            when(accountRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = adminService.setAccountStatus(999, false);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertTrue(response.getBody().getMessage().contains("Không tìm thấy tài khoản ID: 999"));
        }
    }

    @Nested
    @DisplayName("Change Account Status")
    class ChangeAccountStatusTests {

        @Test
        @DisplayName("Disable active account returns OK")
        void setAccountStatus_disable_returnsOk() {
            when(accountRepository.findById(1)).thenReturn(Optional.of(sampleAccount));
            when(accountRepository.save(any())).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = adminService.setAccountStatus(1, false);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Vô hiệu hoá tài khoản thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Enable inactive account returns OK")
        void setAccountStatus_enable_returnsOk() {
            sampleAccount.setIsActive(false);
            when(accountRepository.findById(1)).thenReturn(Optional.of(sampleAccount));
            when(accountRepository.save(any())).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = adminService.setAccountStatus(1, true);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Kích hoạt tài khoản thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent account returns NOT_FOUND")
        void setAccountStatus_notFound_returnsNotFound() {
            when(accountRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = adminService.setAccountStatus(999, true);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertTrue(response.getBody().getMessage().contains("Không tìm thấy tài khoản ID: 999"));
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void setAccountStatus_exception_returnsServerError() {
            when(accountRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = adminService.setAccountStatus(1, true);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Update Account Role")
    class UpdateAccountRoleTests {

        @Test
        @DisplayName("Valid role change returns OK")
        void changeRole_valid_returnsOk() {
            when(accountRepository.findById(1)).thenReturn(Optional.of(sampleAccount));
            when(investorRepository.findByAccount_AccountId(1)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = adminService.changeRole(1, "Investor");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Đổi role thành công", response.getBody().getMessage());
            verify(investorRepository).save(any());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void changeRole_exception_returnsServerError() {
            when(accountRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = adminService.changeRole(1, "Investor");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent account returns NOT_FOUND")
        void changeRole_notFound_returnsNotFound() {
            when(accountRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = adminService.changeRole(999, "Investor");

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Không tìm thấy tài khoản ID: 999", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Invalid role returns BAD_REQUEST")
        void changeRole_invalidRole_returnsBadRequest() {
            when(accountRepository.findById(1)).thenReturn(Optional.of(sampleAccount));

            ResponseEntity<ApiResponse> response = adminService.changeRole(1, "INVALID_ROLE");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Role không hợp lệ: INVALID_ROLE. Các role hợp lệ: Investor, Seller, Staff, Admin", response.getBody().getMessage());
        }
    }


}