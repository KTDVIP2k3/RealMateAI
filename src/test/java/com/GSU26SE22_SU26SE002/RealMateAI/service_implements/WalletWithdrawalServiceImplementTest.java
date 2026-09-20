package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Wallet;
import com.GSU26SE22_SU26SE002.RealMateAI.model.WalletWithdrawal;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WalletWithdrawalRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletWithdrawalServiceImplement - Wallet Withdrawal Management")
class WalletWithdrawalServiceImplementTest {

    @Mock
    private WalletWithdrawalRepository walletWithdrawalRepository;

    @Mock
    private AuthenUntil authenUntil;

    @InjectMocks
    private WalletWithdrawalServiceImplement walletWithdrawalService;

    private Account sampleAccount;
    private WalletWithdrawal sampleWithdrawal;
    private List<WalletWithdrawal> sampleWithdrawals;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setAccountId(1);
        sampleAccount.setFull_name("Test User");
        sampleAccount.setPhone("0123456789");

        Wallet sampleWallet = Wallet.builder().walletId(1).account(sampleAccount).build();

        sampleWithdrawal = WalletWithdrawal.builder()
                .walletWithdrawalId(1)
                .wallet(sampleWallet)
                .amount(new BigDecimal("100000"))
                .status("PENDING")
                .bankName("VCB")
                .bankAccountNumber("12345")
                .createdAt(LocalDateTime.now())
                .build();

        sampleWithdrawals = new ArrayList<>();
        sampleWithdrawals.add(sampleWithdrawal);
    }

    @Nested
    @DisplayName("View Wallet Withdrawals")
    class ViewWalletWithdrawalsTests {

        @Test
        @DisplayName("Valid request returns OK")
        void getWalletWithdrawalByInvestorOrSeller_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletWithdrawalRepository.findAll()).thenReturn(sampleWithdrawals);

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByInvestorOrSeller(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Lấy danh sách thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Unauthenticated returns UNAUTHORIZED")
        void getWalletWithdrawalByInvestorOrSeller_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByInvestorOrSeller(0, 10);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Người dùng chưa đăng nhập", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getWalletWithdrawalByInvestorOrSeller_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByInvestorOrSeller(0, 10);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Lỗi hệ thống: DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("View Wallet Withdrawal Requests (Admin)")
    class ViewWalletWithdrawalRequestsTests {

        @Test
        @DisplayName("Valid request returns OK")
        void getWalletWithdrawalByAdmin_valid_returnsOk() {
            sampleAccount.setRole(RoleEnum.Admin);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletWithdrawalRepository.findAll()).thenReturn(sampleWithdrawals);

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByAdmin(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Lấy danh sách thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Unauthenticated returns UNAUTHORIZED")
        void getWalletWithdrawalByAdmin_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByAdmin(0, 10);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Người dùng chưa đăng nhập", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-admin role returns FORBIDDEN")
        void getWalletWithdrawalByAdmin_forbidden_returnsForbidden() {
            sampleAccount.setRole(RoleEnum.Investor);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByAdmin(0, 10);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getWalletWithdrawalByAdmin_exception_returnsServerError() {
            sampleAccount.setRole(RoleEnum.Admin);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletWithdrawalRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByAdmin(0, 10);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Lỗi hệ thống: DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("View Wallet Withdrawal Details")
    class ViewWalletWithdrawalDetailsTests {

        @Test
        @DisplayName("Existent withdrawal returns OK")
        void getWalletWithdrawalDetailById_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletWithdrawalRepository.findById(1)).thenReturn(Optional.of(sampleWithdrawal));

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalDetailById(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Lấy chi tiết yêu cầu rút tiền thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getWalletWithdrawalDetailById_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletWithdrawalRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalDetailById(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Lỗi hệ thống: DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Unauthenticated returns UNAUTHORIZED")
        void getWalletWithdrawalDetailById_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalDetailById(1);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Người dùng chưa đăng nhập", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent withdrawal returns NOT_FOUND")
        void getWalletWithdrawalDetailById_notFound_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletWithdrawalRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalDetailById(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Không tìm thấy yêu cầu rút tiền này", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("View Wallet Withdrawals By Status")
    class ViewWalletWithdrawalsByStatusTests {

        @Test
        @DisplayName("Valid request returns OK")
        void getWalletWithdrawalByInvestorOrSellerByStatus_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletWithdrawalRepository.findAll()).thenReturn(sampleWithdrawals);

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByInvestorOrSellerByStatus(0, 10, "PENDING");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Lấy danh sách thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getWalletWithdrawalByInvestorOrSellerByStatus_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletWithdrawalRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByInvestorOrSellerByStatus(0, 10, "PENDING");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Lỗi hệ thống: DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Unauthenticated returns UNAUTHORIZED")
        void getWalletWithdrawalByInvestorOrSellerByStatus_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByInvestorOrSellerByStatus(0, 10, "PENDING");

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Người dùng chưa đăng nhập", response.getBody().getMessage());
        }
    }
    @Nested
    @DisplayName("View Wallet Withdrawal Request By Status")
    class ViewWalletWithdrawalRequestByStatusTests {

        @Test
        @DisplayName("Valid request returns OK")
        void getWalletWithdrawalByAdminStatus_valid_returnsOk() {
            sampleAccount.setRole(RoleEnum.Admin);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletWithdrawalRepository.findAll()).thenReturn(sampleWithdrawals);

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByAdminStatus(0, 10, "PENDING");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Lấy danh sách thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getWalletWithdrawalByAdminStatus_exception_returnsServerError() {
            sampleAccount.setRole(RoleEnum.Admin);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletWithdrawalRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByAdminStatus(0, 10, "PENDING");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Lỗi hệ thống: DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Unauthenticated returns UNAUTHORIZED")
        void getWalletWithdrawalByAdminStatus_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = walletWithdrawalService.getWalletWithdrawalByAdminStatus(0, 10, "PENDING");

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Người dùng chưa đăng nhập", response.getBody().getMessage());
        }
    }
}