package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.PostingPackageOrderStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageOrderRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PaymentAttemptResult;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PostingPackageOrderDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingVerificationServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PostingPackageOrderServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PostingPackageOrderServiceImplement implements PostingPackageOrderServiceInterface {

    @Autowired
    private PostingPackageOrderRepository postingPackageOrderRepository;

    @Autowired
    private AuthenUntil authenUntil;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PostingPackageRepository postingPackageRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ListingVerificationServiceInterface listingVerificationServiceInterface;

    @Override
    public ResponseEntity<ApiResponse> getPostingPackageOrders(int page, int size) {
        try {
            Account account = authenUntil.getCurrentUSer();
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Account does not exist"));
            }

            List<PostingPackageOrderDTO> postingPackageOrderDTOList = new ArrayList<>();

            if (account.getRole().name().equals(RoleEnum.Seller.name())) {
                if (account.getSeller() == null) {
                    Seller seller = new Seller();
                    seller.setIsActive(true);
                    seller.setAccount(account);
                    seller.setCreatedAt(LocalDateTime.now());
                    sellerRepository.save(seller);
                    return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Posting package order list is empty"));
                }

                for (Listing listing : account.getSeller().getListings()) {
                    for (PostingPackageOrder postingPackageOrder : listing.getPostingPackageOrders()) {
                        PostingPackageOrderDTO postingPackageOrderDTO = new PostingPackageOrderDTO();
                        postingPackageOrderDTO.setPostingPackageOrderId(postingPackageOrder.getPostingPackageOrderId());
                        postingPackageOrderDTO.setPostingPackageId(postingPackageOrder.getPostingPackage().getPostingPackageId());
                        postingPackageOrderDTO.setPostingPackageName(postingPackageOrder.getPostingPackage().getName());
                        postingPackageOrderDTO.setListingId(postingPackageOrder.getListing().getListingId());
                        postingPackageOrderDTO.setListingTitle(postingPackageOrder.getListing().getTitle());
                        postingPackageOrderDTO.setDuration(postingPackageOrder.getDuration());
                        postingPackageOrderDTO.setTotalAmount(postingPackageOrder.getTotalAmount());
                        postingPackageOrderDTO.setStartDate(postingPackageOrder.getStartDate());
                        postingPackageOrderDTO.setEndDate(postingPackageOrder.getEndDate());
                        postingPackageOrderDTO.setIsActive(postingPackageOrder.getIsActive());
                        postingPackageOrderDTO.setStatus(postingPackageOrder.getStatus() != null
                                ? postingPackageOrder.getStatus().name() : null);
                        postingPackageOrderDTOList.add(postingPackageOrderDTO);
                    }
                }
            }

            List<PostingPackageOrderDTO> sortedList = postingPackageOrderDTOList.stream()
                    .sorted(Comparator.comparing(
                            PostingPackageOrderDTO :: getStartDate,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .toList();

            boolean isGetAll = (page == 0 && size == 0);

            List<PostingPackageOrderDTO> pagedContent;
            int effectivePage = 0;
            int effectiveSize = sortedList.size();
            int totalElements = sortedList.size();
            int totalPages = 1;
            boolean isLast = true;

            if (isGetAll) {
                pagedContent = sortedList;
            } else {
                effectiveSize = size > 0 ? size : 20;
                effectivePage = Math.max(page, 0);

                Pageable pageable =
                        PageRequest.of(effectivePage, effectiveSize);

                pagedContent = sortedList.stream()
                        .skip(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .collect(Collectors.toList());

                org.springframework.data.domain.Page<PostingPackageOrderDTO> orderPage =
                        new org.springframework.data.domain.PageImpl<>(pagedContent, pageable, totalElements);

                effectivePage = orderPage.getNumber();
                effectiveSize = orderPage.getSize();
                totalPages = orderPage.getTotalPages();
                isLast = orderPage.isLast();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", pagedContent);
            result.put("page", effectivePage);
            result.put("size", effectiveSize);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("last", isLast);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(result, "Posting package order list"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> payPostingPackage(PostingPackageOrderRequest postingPackageOrderRequest) {
        try {
            PostingPackage postingPackage = postingPackageRepository
                    .findById(postingPackageOrderRequest.getPostingPackageId()).orElse(null);
            if (postingPackage == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Posting package id does not exist"));
            }

            Listing listing = listingRepository.findById(postingPackageOrderRequest.getListingId()).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Listing id does not exist"));
            }

            PaymentAttemptResult result = executePayment(listing, postingPackage,
                    postingPackageOrderRequest.getDuration(), postingPackageOrderRequest.getTotalAmount());

            if (!result.isSuccess()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), result.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Payment for posting package successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
    }


    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public PaymentAttemptResult attemptAutoPaymentForNewListing(Integer listingId, Integer postingPackageId,
                                                                Integer duration, BigDecimal totalAmount) {
        try {
            PostingPackage postingPackage = postingPackageRepository.findById(postingPackageId).orElse(null);
            if (postingPackage == null) {
                return PaymentAttemptResult.fail("POSTING_PACKAGE_NOT_FOUND", "Gói dịch vụ đăng tin không tồn tại");
            }

            Listing listing = listingRepository.findById(listingId).orElse(null);
            if (listing == null) {
                return PaymentAttemptResult.fail("LISTING_NOT_FOUND", "Tin đăng không tồn tại");
            }

            return executePayment(listing, postingPackage, duration, totalAmount);
        } catch (Exception e) {
            log.error("[PostingPackageOrderService] attemptAutoPaymentForNewListing lỗi: listingId={}", listingId, e);
            return PaymentAttemptResult.fail("PAYMENT_ERROR", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SỬA (theo đúng yêu cầu nghiệp vụ): TRƯỚC ĐÂY nếu ví null/không đủ tiền,
    // hàm này return fail NGAY, KHÔNG hề tạo PostingPackageOrder nào cả — nghĩa
    // là không có gì để "thanh toán lại" sau này, Seller phải gọi lại từ đầu
    // (dễ tạo trùng nếu FE gọi lại POST tạo listing). Nay LUÔN tạo + lưu order
    // NGAY TỪ ĐẦU (status=PENDING), sau đó cập nhật lại đúng status thật
    // (SUCCESS/FAILED) tuỳ kết quả — dùng CHUNG cho payPostingPackage(),
    // attemptAutoPaymentForNewListing() (tạo mới), và retryPayPostingPackage()
    // (thanh toán lại — KHÔNG tạo order mới, chỉ update lại đúng order này).
    //
    // MỚI: LUÔN ghi lại 1 dòng Transaction (bảng "transaction") cho MỌI lần
    // thử thanh toán — kể cả khi THẤT BẠI (transactionStatus="FAILED") —
    // không chỉ khi thành công như trước. Đây là bước "transaction" investor/
    // Seller yêu cầu bổ sung: có đầy đủ lịch sử mọi lần thử thanh toán (kể cả
    // fail) để tra soát sau này, không chỉ có mỗi các lần SUCCESS.
    // ════════════════════════════════════════════════════════════════════════
    private PaymentAttemptResult executePayment(Listing listing, PostingPackage postingPackage,
                                                Integer duration, BigDecimal totalAmount) {
        Account account = listing.getSeller().getAccount();
        int accountId = account.getAccountId();
        LocalDateTime now = LocalDateTime.now();

        // MỚI: tạo order NGAY, mặc định FAILED — chỉ nâng lên SUCCESS nếu toàn
        // bộ bước thanh toán bên dưới trót lọt. Nhờ vậy dù ví null/không đủ
        // tiền, order vẫn tồn tại (status=FAILED) để Seller gọi retry-pay sau.
        PostingPackageOrder order = new PostingPackageOrder();
        order.setPostingPackage(postingPackage);
        order.setListing(listing);
        order.setDuration(duration);
        order.setTotalAmount(totalAmount);
        order.setIsActive(false);
        order.setStartDate(null);
        order.setEndDate(null);
        order.setStatus(PostingPackageOrderStatusEnum.FAILED);
        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        Wallet wallet = walletRepository.findByAccount_AccountId(accountId).orElse(null);
        if (wallet == null) {
            PostingPackageOrder saved = postingPackageOrderRepository.save(order);
            // MỚI: ghi Transaction FAILED — wallet=null vì tài khoản chưa có ví,
            // vẫn ghi được vì cột wallet_id ở DB cho phép NULL.
            saveTransactionRecord(null, account, totalAmount, "FAILED",
                    "Thanh toán gói dịch vụ đăng tin THẤT BẠI — tài khoản chưa có ví",
                    "PAY_FAIL_" + System.currentTimeMillis(), now);
            return PaymentAttemptResult.fail("WALLET_NOT_FOUND",
                    "Tài khoản chưa có ví — vui lòng tạo/nạp ví trước khi thanh toán gói dịch vụ đăng tin",
                    saved.getPostingPackageOrderId());
        }

        if (wallet.getBalance().compareTo(totalAmount) < 0) {
            PostingPackageOrder saved = postingPackageOrderRepository.save(order);
            // MỚI: ghi Transaction FAILED — KHÔNG trừ tiền ví (chưa đủ).
            saveTransactionRecord(wallet, account, totalAmount, "FAILED",
                    "Thanh toán gói dịch vụ đăng tin THẤT BẠI — số dư ví không đủ (còn " + wallet.getBalance() + ")",
                    "PAY_FAIL_" + System.currentTimeMillis(), now);
            return PaymentAttemptResult.fail("INSUFFICIENT_BALANCE",
                    "Số dư ví không đủ để thanh toán gói dịch vụ đăng tin, vui lòng nạp thêm tiền",
                    saved.getPostingPackageOrderId());
        }

        // Từ đây trở đi: thanh toán THÀNH CÔNG thật sự.
        wallet.setBalance(wallet.getBalance().subtract(totalAmount));
        wallet.setUpdatedAt(now);

        // KHÔNG set thẳng isActive/startDate/endDate ở đây — tách riêng ra
        // transitionListingForNewPackageOrder() (xem javadoc bên dưới).
        transitionListingForNewPackageOrder(listing, order, now);
        order.setStatus(PostingPackageOrderStatusEnum.SUCCESS);

        saveTransactionRecord(wallet, account, totalAmount, "SUCCESS",
                "Thanh toán gói dịch vụ đăng tin RealMateAI",
                "PAY_" + System.currentTimeMillis(), now);

        walletRepository.save(wallet);
        PostingPackageOrder savedOrder = postingPackageOrderRepository.save(order);

        return PaymentAttemptResult.ok(savedOrder.getPostingPackageOrderId());
    }

    // MỚI: hàm dùng CHUNG để ghi 1 dòng Transaction — cho cả SUCCESS lẫn FAILED,
    // dùng ở executePayment() VÀ retryPayPostingPackage() để không viết lặp lại
    // cùng 1 đoạn code build Transaction ở nhiều nơi.
    private void saveTransactionRecord(Wallet wallet, Account account, BigDecimal totalAmount,
                                       String status, String description, String transactionCode,
                                       LocalDateTime now) {
        Transaction transaction = Transaction.builder()
                .wallet(wallet) // có thể null nếu tài khoản chưa có ví (WALLET_NOT_FOUND)
                .account(account)
                .transactionType(TransactionTypeEnum.POSTING_PACKAGE_PAYMENT)
                .transactionDate(now)
                .totalAmount(totalAmount != null ? totalAmount.longValue() : null)
                .transactionCode(transactionCode)
                .contentDescription(description)
                .transactionStatus(status)
                .createdAt(now)
                .build();
        transactionRepository.save(transaction);
    }

    @Override
    public ResponseEntity<ApiResponse> renewPostingPackage(Integer postingPackageOrderId) {
        try {
            PostingPackageOrder postingPackageOrder = postingPackageOrderRepository.findById(postingPackageOrderId).orElse(null);
            if (postingPackageOrder == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Posting package order id does not exist!!!"));
            }

            Account account = postingPackageOrder.getListing().getSeller().getAccount();
            int accountId = account.getAccountId();

            Wallet wallet = walletRepository.findByAccount_AccountId(accountId).orElse(null);
            if (wallet == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Wallet does not exists, please deposit it"));
            }

            BigDecimal totalAmount = postingPackageOrder.getTotalAmount();
            if (wallet.getBalance().compareTo(totalAmount) < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Insufficient wallet balance"));
            }

            LocalDateTime startDate = LocalDateTime.now();
            int durationDays = postingPackageOrder.getDuration();

            postingPackageOrder.setIsActive(true);
            postingPackageOrder.setStartDate(startDate);
            postingPackageOrder.setEndDate(startDate.plusDays(durationDays));
            postingPackageOrder.setUpdatedAt(startDate);

            wallet.setBalance(wallet.getBalance().subtract(totalAmount));
            wallet.setUpdatedAt(startDate);

            Transaction transaction = Transaction.builder()
                    .wallet(wallet)
                    .account(account)
                    .transactionType(TransactionTypeEnum.POSTING_PACKAGE_RENEWAL)
                    .transactionDate(startDate)
                    .totalAmount(totalAmount.longValue())
                    .transactionCode("RENEW_" + System.currentTimeMillis())
                    .contentDescription("Gia hạn gói dịch vụ đăng tin RealMateAI")
                    .transactionStatus("SUCCESS")
                    .createdAt(startDate)
                    .build();

            transactionRepository.save(transaction);
            walletRepository.save(wallet);
            postingPackageOrderRepository.save(postingPackageOrder);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Renewal posting package successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // MỚI: Thanh toán LẠI cho 1 PostingPackageOrder đã tồn tại nhưng đang
    // status=FAILED (ví null/không đủ tiền lúc tạo lần đầu). KHÔNG tạo order
    // mới — chỉ cập nhật LẠI đúng order này. Sau khi thành công: status
    // chuyển SUCCESS, Listing chuyển WAITING_PAYMENT -> PENDING (hoặc kích
    // hoạt ngay nếu Listing đã APPROVED) — y hệt luồng thanh toán lần đầu.
    // ════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> retryPayPostingPackage(Integer postingPackageOrderId) {
        try {
            PostingPackageOrder order = postingPackageOrderRepository.findById(postingPackageOrderId).orElse(null);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Posting package order id does not exist"));
            }

            // Chỉ đúng Seller sở hữu Listing của order này mới được thanh toán lại.
            Account currentUser = authenUntil.getCurrentUSer();
            if (currentUser == null || order.getListing() == null || order.getListing().getSeller() == null
                    || order.getListing().getSeller().getAccount().getAccountId() != currentUser.getAccountId()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail(HttpStatus.FORBIDDEN.toString(), "Bạn không có quyền thanh toán lại đơn hàng này"));
            }

            if (order.getStatus() == PostingPackageOrderStatusEnum.SUCCESS) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Đơn hàng này đã thanh toán thành công trước đó"));
            }

            Listing listing = order.getListing();
            Account sellerAccount = listing.getSeller().getAccount();

            Wallet wallet = walletRepository.findByAccount_AccountId(sellerAccount.getAccountId()).orElse(null);
            if (wallet == null) {
                // MỚI: ghi Transaction FAILED cho lần retry này — có đầy đủ lịch sử
                // mọi lần thử thanh toán, kể cả retry vẫn thất bại.
                saveTransactionRecord(null, sellerAccount, order.getTotalAmount(), "FAILED",
                        "Thanh toán LẠI gói dịch vụ đăng tin THẤT BẠI — tài khoản chưa có ví",
                        "PAY_RETRY_FAIL_" + System.currentTimeMillis(), LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(),
                                "Tài khoản chưa có ví — vui lòng tạo/nạp ví trước khi thanh toán lại"));
            }

            BigDecimal totalAmount = order.getTotalAmount();
            if (wallet.getBalance().compareTo(totalAmount) < 0) {
                // MỚI: ghi Transaction FAILED — KHÔNG trừ tiền ví (chưa đủ).
                saveTransactionRecord(wallet, sellerAccount, totalAmount, "FAILED",
                        "Thanh toán LẠI gói dịch vụ đăng tin THẤT BẠI — số dư ví không đủ (còn " + wallet.getBalance() + ")",
                        "PAY_RETRY_FAIL_" + System.currentTimeMillis(), LocalDateTime.now());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(),
                                "Số dư ví không đủ để thanh toán gói dịch vụ đăng tin, vui lòng nạp thêm tiền"));
            }

            LocalDateTime now = LocalDateTime.now();
            wallet.setBalance(wallet.getBalance().subtract(totalAmount));
            wallet.setUpdatedAt(now);

            // order đã có sẵn duration -> transitionListingForNewPackageOrder() dùng
            // lại đúng order NÀY (update, không tạo mới).
            transitionListingForNewPackageOrder(listing, order, now);
            order.setStatus(PostingPackageOrderStatusEnum.SUCCESS);
            order.setUpdatedAt(now);

            saveTransactionRecord(wallet, sellerAccount, totalAmount, "SUCCESS",
                    "Thanh toán lại gói dịch vụ đăng tin RealMateAI",
                    "PAY_RETRY_" + System.currentTimeMillis(), now);

            walletRepository.save(wallet);
            postingPackageOrderRepository.save(order);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Thanh toán lại gói dịch vụ đăng tin thành công"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
    }

    private void transitionListingForNewPackageOrder(Listing listing,
                                                     PostingPackageOrder postingPackageOrder,
                                                     LocalDateTime startDate) {
        boolean listingAlreadyApproved = listingVerificationServiceInterface.transitionToPendingOnPayment(listing);

        if (listingAlreadyApproved) {
            postingPackageOrder.setIsActive(true);
            postingPackageOrder.setStartDate(startDate);
            postingPackageOrder.setEndDate(startDate.plusDays(postingPackageOrder.getDuration()));
        } else {
            postingPackageOrder.setIsActive(false);
            postingPackageOrder.setStartDate(null);
            postingPackageOrder.setEndDate(null);
        }

        // MỚI: nâng priority hiển thị của Listing theo gói VỪA thanh toán thành
        // công (hàm này chỉ được gọi khi thanh toán ĐÃ trót lọt — xem
        // executePayment/retryPayPostingPackage). Chỉ NÂNG lên nếu gói mới cao
        // hơn priority hiện có — Seller mua thêm 1 gói thấp hơn gói đang có sẵn
        // thì KHÔNG được hạ priority đang tốt hơn xuống.
        Integer newPriority = postingPackageOrder.getPostingPackage().getPriority() != null
                ? postingPackageOrder.getPostingPackage().getPriority().intValue() : 0;
        int currentPriority = listing.getPriority() != null ? listing.getPriority() : 0;
        if (newPriority > currentPriority) {
            listing.setPriority(newPriority);
        }
    }
}