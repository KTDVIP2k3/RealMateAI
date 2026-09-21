package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.PostingPackageOrderStatusEnum;
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
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

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
        @DisplayName("Thanh toán gói đăng tin thành công khi dữ liệu hợp lệ và ví đủ tiền")
        void pay_validPackageAndListing_returnsSuccess() {
            Seller seller = Seller.builder()
                    .sellerId(10)
                    .account(sellerAccount)
                    .build();
            sellerAccount.setSeller(seller);

            PostingPackage postingPackage = PostingPackage.builder()
                    .postingPackageId(1)
                    .build();

            Listing listing = Listing.builder()
                    .listingId(100)
                    .seller(seller)
                    .build();

            Wallet wallet = new Wallet();
            wallet.setBalance(new BigDecimal("500000"));

            when(postingPackageRepository.findById(1))
                    .thenReturn(Optional.of(postingPackage));

            when(listingRepository.findById(100))
                    .thenReturn(Optional.of(listing));

            when(walletRepository.findByAccount_AccountId(1))
                    .thenReturn(Optional.of(wallet));

            when(listingVerificationServiceInterface
                    .transitionToPendingOnPayment(listing))
                    .thenReturn(false);

            when(postingPackageOrderRepository
                    .save(any(PostingPackageOrder.class)))
                    .thenAnswer(invocation -> {
                        PostingPackageOrder savedOrder = invocation.getArgument(0);
                        savedOrder.setPostingPackageOrderId(1);
                        return savedOrder;
                    });

            PostingPackageOrderRequest req = new PostingPackageOrderRequest();
            req.setPostingPackageId(1);
            req.setListingId(100);
            req.setDuration(30);
            req.setTotalAmount(new BigDecimal("100000"));

            ResponseEntity<ApiResponse> response =
                    postingPackageOrderService.payPostingPackage(req);

            assertEquals(HttpStatus.OK, response.getStatusCode());

            verify(transactionRepository).save(any(Transaction.class));
            verify(walletRepository).save(wallet);
            verify(listingRepository).save(listing);
            verify(postingPackageOrderRepository)
                    .save(any(PostingPackageOrder.class));
        }
        @Test
        @DisplayName("Trả 404 khi postingPackageId không tồn tại")
        void pay_packageNotFound_return_Not_Found() {
            when(postingPackageRepository.findById(999)).thenReturn(Optional.empty());
            PostingPackageOrderRequest req = new PostingPackageOrderRequest();
            req.setPostingPackageId(999);

            ResponseEntity<ApiResponse> response = postingPackageOrderService.payPostingPackage(req);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 404 khi listingId không tồn tại")
        void pay_listingNotFound_returnsNotFound() {
            PostingPackage pkg = PostingPackage.builder().postingPackageId(1).build();
            when(postingPackageRepository.findById(1)).thenReturn(Optional.of(pkg));
            when(listingRepository.findById(999)).thenReturn(Optional.empty());
            PostingPackageOrderRequest req = new PostingPackageOrderRequest();
            req.setPostingPackageId(1);
            req.setListingId(999);

            ResponseEntity<ApiResponse> response = postingPackageOrderService.payPostingPackage(req);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    // ── 91. Retry Posting Package Payment ─────────────────────────────────
    @Nested
    @DisplayName("retryPayPostingPackage")
    class RetryPayPostingPackageTests {
        @Test
        @DisplayName("Thanh toán lại gói đăng tin thành công khi Order hợp lệ và ví đủ tiền")
        void retry_failedOrderAndSufficientBalance_returnsOk() {
            Seller seller = Seller.builder()
                    .sellerId(10)
                    .account(sellerAccount)
                    .build();
            sellerAccount.setSeller(seller);

            PostingPackage postingPackage = PostingPackage.builder()
                    .postingPackageId(1)
                    .build();

            Listing listing = Listing.builder()
                    .listingId(100)
                    .seller(seller)
                    .build();

            PostingPackageOrder order = new PostingPackageOrder();
            order.setPostingPackageOrderId(1);
            order.setPostingPackage(postingPackage);
            order.setListing(listing);
            order.setDuration(30);
            order.setTotalAmount(new BigDecimal("100000"));
            order.setStatus(PostingPackageOrderStatusEnum.FAILED);

            Wallet wallet = new Wallet();
            wallet.setBalance(new BigDecimal("500000"));

            when(postingPackageOrderRepository.findById(1))
                    .thenReturn(Optional.of(order));

            when(authenUntil.getCurrentUSer())
                    .thenReturn(sellerAccount);

            when(walletRepository.findByAccount_AccountId(1))
                    .thenReturn(Optional.of(wallet));

            when(listingVerificationServiceInterface
                    .transitionToPendingOnPayment(listing))
                    .thenReturn(false);

            ResponseEntity<ApiResponse> response =
                    postingPackageOrderService.retryPayPostingPackage(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());

            verify(transactionRepository).save(any(Transaction.class));
            verify(walletRepository).save(wallet);
            verify(listingRepository).save(listing);
            verify(postingPackageOrderRepository).save(order);
        }



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
        @DisplayName("Gia hạn gói đăng tin thành công khi Order hợp lệ và ví đủ tiền")
        void renew_existingOrderAndSufficientBalance_returnsOk() {
            Seller seller = Seller.builder()
                    .sellerId(10)
                    .account(sellerAccount)
                    .build();
            sellerAccount.setSeller(seller);

            Listing listing = Listing.builder()
                    .listingId(100)
                    .seller(seller)
                    .build();

            PostingPackageOrder order = new PostingPackageOrder();
            order.setPostingPackageOrderId(1);
            order.setListing(listing);
            order.setDuration(30);
            order.setTotalAmount(new BigDecimal("100000"));
            order.setIsActive(false);

            Wallet wallet = new Wallet();
            wallet.setBalance(new BigDecimal("500000"));

            when(postingPackageOrderRepository.findById(1))
                    .thenReturn(Optional.of(order));

            when(walletRepository.findByAccount_AccountId(1))
                    .thenReturn(Optional.of(wallet));

            ResponseEntity<ApiResponse> response =
                    postingPackageOrderService.renewPostingPackage(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());

            verify(transactionRepository).save(any(Transaction.class));
            verify(walletRepository).save(wallet);
            verify(postingPackageOrderRepository).save(order);
        }

        @Test
        @DisplayName("Trả lỗi khi order không tồn tại")
        void renew_orderNotFound_returnsError() {
            when(postingPackageOrderRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = postingPackageOrderService.renewPostingPackage(999);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}
