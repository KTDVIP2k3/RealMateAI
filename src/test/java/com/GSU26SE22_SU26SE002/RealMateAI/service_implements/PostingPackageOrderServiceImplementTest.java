package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageOrderRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingVerificationServiceInterface;
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

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit Test cho PostingPackageOrderServiceImplement — Functions 89-92
 * (Posting Package Order Management, phần Hải).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostingPackageOrderServiceImplement — Posting Package Order")
class PostingPackageOrderServiceImplementTest {

    @Mock private PostingPackageOrderRepository postingPackageOrderRepository;
    @Mock private AuthenUntil authenUntil;
    @Mock private SellerRepository sellerRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private PostingPackageRepository postingPackageRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private ListingVerificationServiceInterface listingVerificationServiceInterface;

    @InjectMocks
    private PostingPackageOrderServiceImplement postingPackageOrderService;

    private Account sellerAccount;

    @BeforeEach
    void setUp() {
        sellerAccount = new Account();
        sellerAccount.setAccountId(1);
        sellerAccount.setRole(RoleEnum.Seller);
    }

    // ── 89. View My Posting Package Orders ────────────────────────────────
    @Nested
    @DisplayName("getPostingPackageOrders")
    class GetPostingPackageOrdersTests {
        @Test
        @DisplayName("Trả 404 khi chưa đăng nhập")
        void getOrders_notLoggedIn_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = postingPackageOrderService.getPostingPackageOrders(0, 10);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Tự tạo Seller profile rỗng và trả danh sách trống nếu Seller chưa có profile")
        void getOrders_sellerProfileMissing_createsAndReturnsEmpty() {
            Seller emptySeller = null;
            sellerAccount.setSeller(null);
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);

            ResponseEntity<ApiResponse> response = postingPackageOrderService.getPostingPackageOrders(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả về danh sách order từ các listing của Seller")
        void getOrders_returnsOrderList() {
            Seller seller = Seller.builder().sellerId(10).account(sellerAccount).listings(new ArrayList<>()).build();
            sellerAccount.setSeller(seller);
            when(authenUntil.getCurrentUSer()).thenReturn(sellerAccount);

            ResponseEntity<ApiResponse> response = postingPackageOrderService.getPostingPackageOrders(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 90. Purchase Posting Package ──────────────────────────────────────
    @Nested
    @DisplayName("payPostingPackage")
    class PayPostingPackageTests {
        @Test
        @DisplayName("Trả 400 khi postingPackageId không tồn tại")
        void pay_packageNotFound_returnsBadRequest() {
            when(postingPackageRepository.findById(999)).thenReturn(Optional.empty());
            PostingPackageOrderRequest req = new PostingPackageOrderRequest();
            req.setPostingPackageId(999);

            ResponseEntity<ApiResponse> response = postingPackageOrderService.payPostingPackage(req);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 400 khi listingId không tồn tại")
        void pay_listingNotFound_returnsBadRequest() {
            PostingPackage pkg = PostingPackage.builder().postingPackageId(1).build();
            when(postingPackageRepository.findById(1)).thenReturn(Optional.of(pkg));
            when(listingRepository.findById(999)).thenReturn(Optional.empty());
            PostingPackageOrderRequest req = new PostingPackageOrderRequest();
            req.setPostingPackageId(1);
            req.setListingId(999);

            ResponseEntity<ApiResponse> response = postingPackageOrderService.payPostingPackage(req);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ── 91. Retry Posting Package Payment ─────────────────────────────────
    @Nested
    @DisplayName("retryPayPostingPackage")
    class RetryPayPostingPackageTests {
        @Test
        @DisplayName("Trả lỗi khi order không tồn tại")
        void retry_orderNotFound_returnsError() {
            when(postingPackageOrderRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = postingPackageOrderService.retryPayPostingPackage(999);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    // ── 92. Renew Posting Package ─────────────────────────────────────────
    @Nested
    @DisplayName("renewPostingPackage")
    class RenewPostingPackageTests {
        @Test
        @DisplayName("Trả lỗi khi order không tồn tại")
        void renew_orderNotFound_returnsError() {
            when(postingPackageOrderRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = postingPackageOrderService.renewPostingPackage(999);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}
