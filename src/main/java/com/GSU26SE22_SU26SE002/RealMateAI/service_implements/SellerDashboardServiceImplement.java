package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.PostingPackageOrderStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PostingPackageOrder;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Transaction;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Wallet;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PostingPackageOrderRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.TransactionRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WalletRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PostingPackageOrderDtoV2;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.TransactionSummaryDto;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.WalletSummaryDto;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.SellerDashboardServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SellerDashboardServiceImplement implements SellerDashboardServiceInterface {

    @Autowired
    private PostingPackageOrderRepository postingPackageOrderRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    private Integer getCurrentAccountId() {
        return 88;
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getPostingPackageOrders(Boolean activeOnly, Integer limit) {
        try {
            Integer currentAccountId = getCurrentAccountId();
            List<PostingPackageOrder> allOrders = postingPackageOrderRepository.findAll();
            if (allOrders == null) {
                allOrders = Collections.emptyList();
            }

            LocalDateTime now = LocalDateTime.now();

            List<PostingPackageOrderDtoV2> filteredOrders = allOrders.stream()
                    .filter(order -> order.getListing() != null
                            && order.getListing().getSeller() != null
                            && order.getListing().getSeller().getAccount() != null
                            && currentAccountId.equals(order.getListing().getSeller().getAccount().getAccountId()))
                    .filter(order -> !Boolean.TRUE.equals(activeOnly) || (Boolean.TRUE.equals(order.getIsActive()) && order.getStatus() == PostingPackageOrderStatusEnum.SUCCESS))
                    .sorted(Comparator.comparing(PostingPackageOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limit != null && limit > 0 ? limit : Long.MAX_VALUE)
                    .map(order -> {
                        PostingPackageOrderDtoV2 dto = new PostingPackageOrderDtoV2();
                        dto.setPostingPackageOrderId(order.getPostingPackageOrderId());

                        if (order.getPostingPackage() != null) {
                            dto.setPostingPackageId(order.getPostingPackage().getPostingPackageId());
                            dto.setPostingPackageName(order.getPostingPackage().getName());
                        }

                        if (order.getListing() != null) {
                            dto.setListingId(order.getListing().getListingId());
                            dto.setListingTitle(order.getListing().getTitle());
                        }

                        dto.setTotalAmount(order.getTotalAmount());
                        dto.setStartDate(order.getStartDate());
                        dto.setEndDate(order.getEndDate());
                        dto.setDuration(order.getDuration());
                        dto.setIsActive(order.getIsActive());
                        dto.setStatus(order.getStatus() != null ? order.getStatus().name() : "PENDING");

                        if (order.getEndDate() != null && order.getEndDate().isAfter(now)) {
                            long days = Duration.between(now, order.getEndDate()).toDays();
                            dto.setDaysRemaining(days);
                            dto.setIsExpiringSoon(days <= 3);
                        } else {
                            dto.setDaysRemaining(0L);
                            dto.setIsExpiringSoon(false);
                        }
                        return dto;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(filteredOrders, "Get seller posting package orders successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getWalletSummary() {
        try {
            Integer currentAccountId = getCurrentAccountId();

            Wallet wallet = walletRepository.findAll().stream()
                    .filter(w -> Boolean.TRUE.equals(w.getIsActive())
                            && w.getAccount() != null
                            && currentAccountId.equals(w.getAccount().getAccountId()))
                    .findFirst()
                    .orElse(null);

            BigDecimal balance = (wallet != null && wallet.getBalance() != null) ? wallet.getBalance() : BigDecimal.ZERO;

            List<Transaction> allTransactions = transactionRepository.findAll();
            List<TransactionSummaryDto> recentTransactions = allTransactions.stream()
                    .filter(t -> t.getAccount() != null && currentAccountId.equals(t.getAccount().getAccountId()))
                    .sorted(Comparator.comparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(5)
                    .map(t -> {
                        TransactionSummaryDto txDto = new TransactionSummaryDto();
                        txDto.setTransactionId(t.getTransactionId());
                        txDto.setAmount(t.getTotalAmount());
                        txDto.setType(t.getTransactionType() != null ? t.getTransactionType().name() : "UNKNOWN");
                        txDto.setCreatedAt(t.getCreatedAt());
                        return txDto;
                    })
                    .collect(Collectors.toList());

            WalletSummaryDto summary = new WalletSummaryDto();
            summary.setBalance(balance);
            summary.setCurrency("VND");
            summary.setRecentTransactions(recentTransactions);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(summary, "Get wallet summary successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}