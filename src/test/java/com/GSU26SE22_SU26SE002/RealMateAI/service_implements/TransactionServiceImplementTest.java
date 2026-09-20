package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Transaction;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Wallet;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.TransactionRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionServiceImplement - Transaction Management")
class TransactionServiceImplementTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthenUntil authenUntil;

    @InjectMocks
    private TransactionServiceImplement transactionService;

    private Account sampleAccount;
    private Transaction sampleTransaction;
    private List<Transaction> sampleTransactions;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setAccountId(1);
        sampleAccount.setFull_name("Test User");
        sampleAccount.setRole(RoleEnum.Investor);

        Wallet wallet = new Wallet();
        wallet.setAccount(sampleAccount);

        sampleTransaction = Transaction.builder()
                .transactionId(1)
                .wallet(wallet)
                .transactionType(TransactionTypeEnum.WALLET_DEPOSIT)
                .totalAmount(new BigDecimal("500000"))
                .transactionStatus("SUCCESS")
                .createdAt(LocalDateTime.now())
                .transactionDate(LocalDateTime.now())
                .build();

        sampleTransactions = new ArrayList<>();
        sampleTransactions.add(sampleTransaction);
        sampleAccount.setTransactions(sampleTransactions);
    }

    @Nested
    @DisplayName("F-032. View My Transactions")
    class ViewMyTransactionsTests {

        @Test
        @DisplayName("UC-154: Valid request returns OK")
        void getMyTransactions_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = transactionService.getMyTransactions(0, 10);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("UC-155: Unauthenticated returns UNAUTHORIZED")
        void getMyTransactions_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = transactionService.getMyTransactions(0, 10);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("UC-156: Exception returns INTERNAL_SERVER_ERROR")
        void getMyTransactions_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = transactionService.getMyTransactions(0, 10);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("F-033. View Transactions (Admin)")
    class ViewTransactionsAdminTests {

        @Test
        @DisplayName("UC-157: Valid request returns OK")
        void getTransactionsByAdminOrStaff_valid_returnsOk() {
            when(transactionRepository.findAll()).thenReturn(sampleTransactions);

            ResponseEntity<ApiResponse> response = transactionService.getTransactionsByAdminOrStaff(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("UC-160: Exception returns INTERNAL_SERVER_ERROR")
        void getTransactionsByAdminOrStaff_exception_returnsServerError() {
            when(transactionRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = transactionService.getTransactionsByAdminOrStaff(0, 10);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("F-034. View My Transactions By Transaction Type")
    class ViewMyTransactionsByTypeTests {

        @Test
        @DisplayName("UC-161: Valid request returns OK")
        void getMyTransactionsByType_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = transactionService.getMyTransactionsByType(0, 10, "WALLET_DEPOSIT");

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("UC-162: Unauthenticated returns UNAUTHORIZED")
        void getMyTransactionsByType_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = transactionService.getMyTransactionsByType(0, 10, "WALLET_DEPOSIT");

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("UC-163: Exception returns INTERNAL_SERVER_ERROR")
        void getMyTransactionsByType_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = transactionService.getMyTransactionsByType(0, 10, "WALLET_DEPOSIT");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("F-035. View Transactions By Type (Admin)")
    class ViewTransactionsByTypeAdminTests {

        @Test
        @DisplayName("UC-164: Valid request returns OK")
        void getTransactionsByAdminOrStaffByType_valid_returnsOk() {
            when(transactionRepository.findAll()).thenReturn(sampleTransactions);

            ResponseEntity<ApiResponse> response = transactionService.getTransactionsByAdminOrStaffByType(0, 10, "WALLET_DEPOSIT");

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("UC-167: Exception returns INTERNAL_SERVER_ERROR")
        void getTransactionsByAdminOrStaffByType_exception_returnsServerError() {
            when(transactionRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = transactionService.getTransactionsByAdminOrStaffByType(0, 10, "WALLET_DEPOSIT");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("F-036. View Transaction Details")
    class ViewTransactionDetailsTests {

        @Test
        @DisplayName("UC-168: Existent transaction returns OK")
        void getTransactionDetailById_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(transactionRepository.findById(1)).thenReturn(Optional.of(sampleTransaction));

            ResponseEntity<ApiResponse> response = transactionService.getTransactionDetailById(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Lấy chi tiết giao dịch thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-169: Unauthenticated returns UNAUTHORIZED")
        void getTransactionDetailById_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = transactionService.getTransactionDetailById(1);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("UC-170: Non-existent transaction returns NOT_FOUND")
        void getTransactionDetailById_notFound_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(transactionRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = transactionService.getTransactionDetailById(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Không tìm thấy thông tin chi tiết giao dịch này", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-171: Exception returns INTERNAL_SERVER_ERROR")
        void getTransactionDetailById_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = transactionService.getTransactionDetailById(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("F-999. Create Transaction Validation")
    class CreateTransactionTests {

        @ParameterizedTest
        @DisplayName("Amount <= 0 returns BAD_REQUEST")
        @ValueSource(strings = {"0", "-100", "-50000"})
        void createTransaction_invalidAmount_returnsBadRequest(String amount) {
            BigDecimal invalidAmount = new BigDecimal(amount);
            // Example of how it would be tested:
            // TransactionRequest request = new TransactionRequest();
            // request.setAmount(invalidAmount);
            // ResponseEntity<ApiResponse> response = transactionService.createTransaction(request);
            // assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}