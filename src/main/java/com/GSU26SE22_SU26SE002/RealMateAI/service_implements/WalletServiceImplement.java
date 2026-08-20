package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.NotificationTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Transaction;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Wallet;
import com.GSU26SE22_SU26SE002.RealMateAI.model.WalletWithdrawal;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.TransactionRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WalletRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WalletWithdrawalRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NotificationService;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.WalletServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WalletServiceImplement implements WalletServiceInterface {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletWithdrawalRepository walletWithdrawalRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuthenUntil authenUntil;

    @Autowired
    private PayOS payOS;

    @Autowired
    private NotificationService notificationService;

    @Value("${payos.return-url}")
    private String returnUrl;

    @Value("${payos.cancel-url}")
    private String cancelUrl;

    private final Map<String, String[]> customUrlCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> initiateDeposit(BigDecimal amount, String customReturnUrl, String customCancelUrl) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            Wallet wallet = walletRepository.findByAccount_AccountId(currentAccount.getAccountId()).orElse(null);

            if (wallet == null) {
                wallet = Wallet.builder()
                        .account(currentAccount)
                        .balance(BigDecimal.ZERO)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                wallet = walletRepository.save(wallet);
            }

            long orderCode = System.currentTimeMillis() / 1000;

            Transaction transaction = Transaction.builder()
                    .wallet(wallet)
                    .account(currentAccount)
                    .transactionType(TransactionTypeEnum.WALLET_DEPOSIT)
                    .transactionDate(LocalDateTime.now())
                    .totalAmount(amount)
                    .transactionCode(String.valueOf(orderCode))
                    .contentDescription("Nạp tiền vào ví RealMateAI")
                    .transactionStatus("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(transaction);

            String finalReturnUrl = returnUrl;
            String finalCancelUrl = cancelUrl;

            if (customReturnUrl != null && !customReturnUrl.isBlank() && customCancelUrl != null && !customCancelUrl.isBlank()) {
                finalReturnUrl = customReturnUrl;
                finalCancelUrl = customCancelUrl;

                customUrlCache.put(String.valueOf(orderCode), new String[]{customReturnUrl, customCancelUrl});
            }

            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount.longValue())
                    .description("Nap tien vi RealMateAI")
                    .returnUrl(finalReturnUrl)
                    .cancelUrl(finalCancelUrl)
                    .build();

            CreatePaymentLinkResponse paymentLink = payOS.paymentRequests().create(paymentRequest);
            String checkoutUrl = paymentLink.getCheckoutUrl();

            transaction.setCheckoutUrl(checkoutUrl);
            transactionRepository.save(transaction);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(checkoutUrl, "Tạo link thanh toán PayOS thành công"));

        } catch (Exception e) {
            System.err.println("=== LỖI PAYOS SDK 2.0.1: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi từ cổng thanh toán: " + e.getMessage()));
        }
    }

    @Override
    public String resolveRedirectUrl(String orderCode, String type) {

        if (customUrlCache.containsKey(orderCode)) {
            String[] urls = customUrlCache.remove(orderCode);
            return "success".equals(type) ? urls[0] : urls[1];
        }

        try {
            Transaction transaction = transactionRepository.findByTransactionCode(orderCode).orElse(null);
            if (transaction != null && transaction.getAccount() != null) {
                String role = transaction.getAccount().getRole().name();

                if ("Seller".equalsIgnoreCase(role)) {
                    return "success".equals(type)
                            ? "http://localhost:3000/seller/wallet?status=success&orderCode=" + orderCode
                            : "http://localhost:3000/seller/wallet?status=cancel&orderCode=" + orderCode;
                } else if ("Investor".equalsIgnoreCase(role)) {
                    return "success".equals(type)
                            ? "mobile://wallet?status=success&orderCode=" + orderCode
                            : "mobile://wallet?status=cancel&orderCode=" + orderCode;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi phân tích định tuyến chuyển hướng: " + e.getMessage());
        }

        return "http://localhost:3000/wallet?status=" + type + "&orderCode=" + orderCode;
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getMyWallet() {
        try {

            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            Wallet wallet = walletRepository.findByAccount_AccountId(currentAccount.getAccountId()).orElse(null);

            if (wallet == null) {
                wallet = Wallet.builder()
                        .account(currentAccount)
                        .balance(BigDecimal.ZERO)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                wallet = walletRepository.save(wallet);
            }

            Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("walletId", wallet.getWalletId());
            responseData.put("balance", wallet.getBalance());
            responseData.put("isActive", wallet.getIsActive());
            responseData.put("userId", wallet.getAccount().getAccountId());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(responseData, "Lấy thông tin ví của tôi thành công"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> handlePayOSWebhook(String orderCode, String status) {
        try {
            Transaction transaction = transactionRepository.findByTransactionCode(orderCode).orElse(null);

            if (transaction == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("NOT_FOUND", "Không tìm thấy mã giao dịch này"));
            }

            if ("PENDING".equals(transaction.getTransactionStatus())) {
                if ("SUCCESS".equals(status)) {
                    transaction.setTransactionStatus("SUCCESS");
                    transactionRepository.save(transaction);

                    Wallet wallet = transaction.getWallet();
                    if (wallet != null && transaction.getTotalAmount() != null) {
//                        BigDecimal amountToDeposit = BigDecimal.valueOf(transaction.getTotalAmount());
                        BigDecimal amountToDeposit = transaction.getTotalAmount();
                        wallet.setBalance(wallet.getBalance().add(amountToDeposit));
                        wallet.setUpdatedAt(LocalDateTime.now());
                        walletRepository.save(wallet);

                        // MỚI: theo mục 6 (Cao) trong RealMateAI_API_Notification_Report.
                        if (wallet.getAccount() != null) {
                            notificationService.notify(wallet.getAccount(),
                                    "Nạp " + transaction.getTotalAmount() + " vào ví thành công.",
                                    NotificationTypeEnum.TRANSACTION);
                        }
                    }

                    return ResponseEntity.status(HttpStatus.OK)
                            .body(ApiResponse.success(null, "Cộng tiền vào ví thành công"));
                }

                if ("CANCELLED".equals(status)) {
                    transaction.setTransactionStatus("CANCELLED");
                    transactionRepository.save(transaction);

                    // MỚI: theo mục 6 (Cao) trong RealMateAI_API_Notification_Report.
                    if (transaction.getWallet() != null && transaction.getWallet().getAccount() != null) {
                        notificationService.notify(transaction.getWallet().getAccount(),
                                "Giao dịch nạp tiền đã bị hủy.",
                                NotificationTypeEnum.TRANSACTION);
                    }

                    return ResponseEntity.status(HttpStatus.OK)
                            .body(ApiResponse.success(null, "Đã cập nhật trạng thái hủy giao dịch thành công"));
                }
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("BAD_REQUEST", "Giao dịch không hợp lệ hoặc đã được xử lý trước đó"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> requestWithdrawal(BigDecimal amount, String bankName, String bankAccountNumber, String note) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("UNAUTHORIZED", "Người dùng chưa đăng nhập"));
            }

            Wallet wallet = walletRepository.findByAccount_AccountId(currentAccount.getAccountId()).orElse(null);

            if (wallet == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("NOT_FOUND", "Không tìm thấy ví của tài khoản này"));
            }

            if (Boolean.FALSE.equals(wallet.getIsActive())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("WALLET_LOCKED", "Ví này hiện đang bị khóa"));
            }

            if (wallet.getBalance().compareTo(amount) < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("INSUFFICIENT_BALANCE", "Số dư khả dụng không đủ"));
            }

            wallet.setBalance(wallet.getBalance().subtract(amount));
            wallet.setUpdatedAt(LocalDateTime.now());
            walletRepository.save(wallet);

            WalletWithdrawal withdrawal = WalletWithdrawal.builder()
                    .wallet(wallet)
                    .amount(amount)
                    .bankName(bankName)
                    .note(note)
                    .bankAccountNumber(bankAccountNumber)
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            withdrawal = walletWithdrawalRepository.save(withdrawal);

            Transaction transaction = Transaction.builder()
                    .wallet(wallet)
                    .account(currentAccount)
                    .transactionType(TransactionTypeEnum.WALLET_WITHDRAWAL)
                    .transactionDate(LocalDateTime.now())
                    .totalAmount(amount)
                    .docnoId(withdrawal.getWalletWithdrawalId())
                    .transactionCode("RUT" + System.currentTimeMillis())
                    .contentDescription("Yêu cầu rút tiền về ngân hàng: " + bankName)
                    .transactionStatus("PENDING")
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(transaction);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Tạo yêu cầu rút tiền thành công, vui lòng chờ duyệt"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("SERVER_ERROR", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> reviewWithdrawRequest(Integer withdrawalId, String status, String reason) {
        try {
            WalletWithdrawal withdrawal = walletWithdrawalRepository.findById(withdrawalId).orElse(null);
            if (withdrawal == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("NOT_FOUND", "Không tìm thấy yêu cầu rút tiền này"));
            }

            String currentStatus = withdrawal.getStatus();

            if ("REJECT".equals(status)) {
                Wallet wallet = withdrawal.getWallet();
                wallet.setBalance(wallet.getBalance().add(withdrawal.getAmount()));
                wallet.setUpdatedAt(LocalDateTime.now());
                walletRepository.save(wallet);

                withdrawal.setStatus("REJECT");
                withdrawal.setReason(reason);
                withdrawal.setUpdatedAt(LocalDateTime.now());
                walletWithdrawalRepository.save(withdrawal);

                transactionRepository.findByTransactionTypeAndDocnoId(TransactionTypeEnum.WALLET_WITHDRAWAL, withdrawalId)
                        .ifPresent(t -> {
                            t.setTransactionStatus("FAILED");
                            transactionRepository.save(t);
                        });

                // MỚI: theo mục 7 (Cao) trong RealMateAI_API_Notification_Report.
                if (wallet.getAccount() != null) {
                    notificationService.notify(wallet.getAccount(),
                            "Yêu cầu rút tiền đã bị TỪ CHỐI, lý do: " + reason + ". Số tiền đã được hoàn lại vào ví.",
                            NotificationTypeEnum.TRANSACTION);
                }

                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Đã từ chối đơn rút tiền và hoàn tiền về ví thành công"));
            }

            if ("APPROVE".equals(status)) {
                withdrawal.setStatus("APPROVE");
                withdrawal.setNote(reason);
                withdrawal.setUpdatedAt(LocalDateTime.now());
                walletWithdrawalRepository.save(withdrawal);

                // MỚI: theo mục 7 (Cao) trong RealMateAI_API_Notification_Report.
                if (withdrawal.getWallet() != null && withdrawal.getWallet().getAccount() != null) {
                    notificationService.notify(withdrawal.getWallet().getAccount(),
                            "Yêu cầu rút tiền đã được PHÊ DUYỆT, đang chờ Staff chuyển khoản.",
                            NotificationTypeEnum.TRANSACTION);
                }

                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Đã phê duyệt đơn rút, chờ Staff chuyển khoản"));
            }

            if ("COMPLETE".equals(status)) {
                if (!"APPROVE".equals(currentStatus) && !"PENDING".equals(currentStatus)) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("BAD_REQUEST", "Đơn hàng phải ở trạng thái PENDING hoặc APPROVE mới có thể hoàn thành"));
                }
                withdrawal.setStatus("COMPLETE");
                withdrawal.setNote(reason);
                withdrawal.setUpdatedAt(LocalDateTime.now());
                walletWithdrawalRepository.save(withdrawal);

                transactionRepository.findByTransactionTypeAndDocnoId(TransactionTypeEnum.WALLET_WITHDRAWAL, withdrawalId)
                        .ifPresent(t -> {
                            t.setTransactionStatus("SUCCESS");
                            transactionRepository.save(t);
                        });

                // MỚI: theo mục 7 (Cao) trong RealMateAI_API_Notification_Report.
                if (withdrawal.getWallet() != null && withdrawal.getWallet().getAccount() != null) {
                    notificationService.notify(withdrawal.getWallet().getAccount(),
                            "Yêu cầu rút tiền đã CHUYỂN KHOẢN THÀNH CÔNG.",
                            NotificationTypeEnum.TRANSACTION);
                }

                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Giao dịch rút tiền đã hoàn tất thành công"));
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("INVALID_STATUS", "Trạng thái chuyển đổi không hợp lệ"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("SERVER_ERROR", e.getMessage()));
        }
    }


}