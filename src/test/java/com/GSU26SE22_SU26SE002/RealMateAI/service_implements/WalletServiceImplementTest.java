package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Wallet;
import com.GSU26SE22_SU26SE002.RealMateAI.model.WalletWithdrawal;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.TransactionRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WalletRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WalletWithdrawalRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NotificationService;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.Answers;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletServiceImplement - Wallet Management")
class WalletServiceImplementTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletWithdrawalRepository walletWithdrawalRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private AuthenUntil authenUntil;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PayOS payOS;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WalletServiceImplement walletService;

    private Account sampleAccount;
    private Wallet sampleWallet;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setAccountId(1);
        sampleAccount.setRole(RoleEnum.Investor);

        sampleWallet = Wallet.builder()
                .walletId(1)
                .account(sampleAccount)
                .balance(new BigDecimal("1000000"))
                .isActive(true)
                .build();
                
        ReflectionTestUtils.setField(walletService, "returnUrl", "http://return.url");
        ReflectionTestUtils.setField(walletService, "cancelUrl", "http://cancel.url");
    }

    @Nested
    @DisplayName("F-024. View My Wallet")
    class ViewMyWalletTests {

        @Test
        @DisplayName("UC-118, UC-121: Wallet exists, returns OK")
        void getMyWallet_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(sampleWallet));

            ResponseEntity<ApiResponse> response = walletService.getMyWallet();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Lấy thông tin ví của tôi thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-119: Unauthenticated returns UNAUTHORIZED")
        void getMyWallet_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = walletService.getMyWallet();

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("Người dùng chưa đăng nhập", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-120: Exception returns INTERNAL_SERVER_ERROR")
        void getMyWallet_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = walletService.getMyWallet();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertTrue(response.getBody().getMessage().contains("Lỗi hệ thống:"));
        }
        
        @Test
        @DisplayName("UC-118b: Wallet not exists, auto creates and returns OK")
        void getMyWallet_walletNotExists_createsAndReturnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> {
                Wallet w = i.getArgument(0);
                w.setWalletId(2);
                return w;
            });

            ResponseEntity<ApiResponse> response = walletService.getMyWallet();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(walletRepository).save(any(Wallet.class));
        }
    }

    @Nested
    @DisplayName("F-025. Deposit My Wallet")
    class DepositMyWalletTests {

//        @Test
//        @DisplayName("UC-122, UC-124, UC-126: Valid deposit with custom URLs returns OK")
//        void initiateDeposit_validWithCustomUrls_returnsOk() throws Exception {
//            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
//            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(sampleWallet));
//            CreatePaymentLinkResponse mockResponse = mock(CreatePaymentLinkResponse.class);
//            when(mockResponse.getCheckoutUrl()).thenReturn("http://payos.link");
//            when(payOS.paymentRequests().create(any(CreatePaymentLinkRequest.class))).thenReturn(mockResponse);
//
//            ResponseEntity<ApiResponse> response = walletService.initiateDeposit(
//                    new BigDecimal("50000"), "http://custom.return", "http://custom.cancel");
//
//            assertEquals(HttpStatus.OK, response.getStatusCode());
//            assertEquals("Tạo link thanh toán PayOS thành công", response.getBody().getMessage());
//        }

        @Test
        @DisplayName("UC-123, UC-125, UC-127: Valid deposit with default URLs returns OK")
        void initiateDeposit_validWithDefaultUrls_returnsOk() throws Exception {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(sampleWallet));
            CreatePaymentLinkResponse mockResponse = mock(CreatePaymentLinkResponse.class);
            when(mockResponse.getCheckoutUrl()).thenReturn("http://payos.link");
            when(payOS.paymentRequests().create(any(CreatePaymentLinkRequest.class))).thenReturn(mockResponse);

            ResponseEntity<ApiResponse> response = walletService.initiateDeposit(
                    new BigDecimal("50000"), null, "");

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Unauthenticated returns UNAUTHORIZED")
        void initiateDeposit_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = walletService.initiateDeposit(
                    new BigDecimal("50000"), null, null);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void initiateDeposit_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = walletService.initiateDeposit(
                    new BigDecimal("50000"), null, null);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @ParameterizedTest
        @DisplayName("Amount <= 0 returns BAD_REQUEST")
        @ValueSource(strings = {"0", "-50000"})
        void initiateDeposit_invalidAmount_returnsBadRequest(String amount) {
            BigDecimal invalidAmount = new BigDecimal(amount);
            ResponseEntity<ApiResponse> response = walletService.initiateDeposit(
                    invalidAmount, "http://return.url", "http://cancel.url");
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("F-026. Withdraw My Wallet")
    class WithdrawMyWalletTests {

        @Test
        @DisplayName("UC-128: Valid withdrawal returns OK")
        void requestWithdrawal_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(sampleWallet));
            when(walletWithdrawalRepository.save(any())).thenAnswer(i -> {
                WalletWithdrawal w = i.getArgument(0);
                w.setWalletWithdrawalId(10);
                return w;
            });

            ResponseEntity<ApiResponse> response = walletService.requestWithdrawal(
                    new BigDecimal("500000"), "Vietcombank", "0123456789", "Withdraw note");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Tạo yêu cầu rút tiền thành công, vui lòng chờ duyệt", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-129: Unauthenticated returns UNAUTHORIZED")
        void requestWithdrawal_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = walletService.requestWithdrawal(
                    new BigDecimal("500000"), "VCB", "123", "note");

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("UC-130: Exception returns INTERNAL_SERVER_ERROR")
        void requestWithdrawal_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = walletService.requestWithdrawal(
                    new BigDecimal("500000"), "VCB", "123", "note");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        @DisplayName("UC-131: Wallet not found returns NOT_FOUND")
        void requestWithdrawal_walletNotFound_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = walletService.requestWithdrawal(
                    new BigDecimal("500000"), "VCB", "123", "note");

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Không tìm thấy ví của tài khoản này", response.getBody().getMessage());
        }
        
        @Test
        @DisplayName("Wallet locked returns BAD_REQUEST")
        void requestWithdrawal_walletLocked_returnsBadRequest() {
            sampleWallet.setIsActive(false);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(sampleWallet));

            ResponseEntity<ApiResponse> response = walletService.requestWithdrawal(
                    new BigDecimal("500000"), "VCB", "123", "note");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Ví này hiện đang bị khóa", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-132: Insufficient balance returns BAD_REQUEST")
        void requestWithdrawal_insufficientBalance_returnsBadRequest() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(sampleWallet));

            ResponseEntity<ApiResponse> response = walletService.requestWithdrawal(
                    new BigDecimal("2000000"), "VCB", "123", "note"); // balance is 1000000

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Số dư khả dụng không đủ", response.getBody().getMessage());
        }

        @ParameterizedTest
        @DisplayName("Amount <= 0 returns BAD_REQUEST")
        @ValueSource(strings = {"0", "-50000"})
        void requestWithdrawal_invalidAmount_returnsBadRequest(String amount) {
            BigDecimal invalidAmount = new BigDecimal(amount);
            ResponseEntity<ApiResponse> response = walletService.requestWithdrawal(invalidAmount, "VCB", "123", "note");
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @ParameterizedTest
        @DisplayName("Blank fields return BAD_REQUEST")
        @CsvSource({
                "'', '123', 'note'",
                "'VCB', '', 'note'"
        })
        void requestWithdrawal_blankFields_returnsBadRequest(String bankName, String bankAccountNumber, String note) {
            ResponseEntity<ApiResponse> response = walletService.requestWithdrawal(
                    new BigDecimal("500000"), bankName, bankAccountNumber, note);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("F-027. Review Wallet Withdrawal Request")
    class ReviewWithdrawRequestTests {

        private WalletWithdrawal sampleWithdrawal;

        @BeforeEach
        void setup() {
            sampleWithdrawal = WalletWithdrawal.builder()
                    .walletWithdrawalId(1)
                    .wallet(sampleWallet)
                    .amount(new BigDecimal("100000"))
                    .status("PENDING")
                    .build();
        }

        @Test
        @DisplayName("UC-133: Valid REJECT returns OK")
        void reviewWithdrawRequest_reject_returnsOk() {
            when(walletWithdrawalRepository.findById(1)).thenReturn(Optional.of(sampleWithdrawal));
            when(walletRepository.save(any())).thenReturn(sampleWallet);
            when(walletWithdrawalRepository.save(any())).thenReturn(sampleWithdrawal);

            ResponseEntity<ApiResponse> response = walletService.reviewWithdrawRequest(1, "REJECT", "Invalid bank");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Đã từ chối đơn rút tiền và hoàn tiền về ví thành công", response.getBody().getMessage());
            verify(notificationService).notify(any(), any(), any());
        }

        @Test
        @DisplayName("UC-134: Exception returns INTERNAL_SERVER_ERROR")
        void reviewWithdrawRequest_exception_returnsServerError() {
            when(walletWithdrawalRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = walletService.reviewWithdrawRequest(1, "REJECT", "Invalid");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        @DisplayName("UC-135: Non-existent withdrawal returns NOT_FOUND")
        void reviewWithdrawRequest_notFound_returnsNotFound() {
            when(walletWithdrawalRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = walletService.reviewWithdrawRequest(999, "REJECT", "Invalid");

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Không tìm thấy yêu cầu rút tiền này", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-136: Invalid status returns BAD_REQUEST")
        void reviewWithdrawRequest_invalidStatus_returnsBadRequest() {
            when(walletWithdrawalRepository.findById(1)).thenReturn(Optional.of(sampleWithdrawal));

            ResponseEntity<ApiResponse> response = walletService.reviewWithdrawRequest(1, "INVALID_STATUS", "Note");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Trạng thái chuyển đổi không hợp lệ", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-137: Valid APPROVE returns OK")
        void reviewWithdrawRequest_approve_returnsOk() {
            when(walletWithdrawalRepository.findById(1)).thenReturn(Optional.of(sampleWithdrawal));
            when(walletWithdrawalRepository.save(any())).thenReturn(sampleWithdrawal);

            ResponseEntity<ApiResponse> response = walletService.reviewWithdrawRequest(1, "APPROVE", "Approved");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Đã phê duyệt đơn rút, chờ Staff chuyển khoản", response.getBody().getMessage());
            verify(notificationService).notify(any(), any(), any());
        }

        @Test
        @DisplayName("UC-138: Valid COMPLETE returns OK")
        void reviewWithdrawRequest_complete_returnsOk() {
            when(walletWithdrawalRepository.findById(1)).thenReturn(Optional.of(sampleWithdrawal));
            when(walletWithdrawalRepository.save(any())).thenReturn(sampleWithdrawal);

            ResponseEntity<ApiResponse> response = walletService.reviewWithdrawRequest(1, "COMPLETE", "Completed");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Giao dịch rút tiền đã hoàn tất thành công", response.getBody().getMessage());
            verify(notificationService).notify(any(), any(), any());
        }

        @Test
        @DisplayName("UC-139: COMPLETE invalid current status returns BAD_REQUEST")
        void reviewWithdrawRequest_completeInvalidCurrentStatus_returnsBadRequest() {
            sampleWithdrawal.setStatus("REJECT");
            when(walletWithdrawalRepository.findById(1)).thenReturn(Optional.of(sampleWithdrawal));

            ResponseEntity<ApiResponse> response = walletService.reviewWithdrawRequest(1, "COMPLETE", "Completed");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Đơn hàng phải ở trạng thái PENDING hoặc APPROVE mới có thể hoàn thành", response.getBody().getMessage());
        }
    }
}
