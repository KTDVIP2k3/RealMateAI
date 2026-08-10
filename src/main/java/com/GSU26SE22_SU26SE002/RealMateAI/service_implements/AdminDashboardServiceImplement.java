package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Transaction;
import com.GSU26SE22_SU26SE002.RealMateAI.model.WalletWithdrawal;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AdminDashboardServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImplement implements AdminDashboardServiceInterface {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletWithdrawalRepository walletWithdrawalRepository;

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getSystemKpis() {
        try {
            List<Account> accounts = accountRepository.findAll();
            if (accounts == null) accounts = Collections.emptyList();

            long totalAccounts = accounts.stream().filter(a -> Boolean.TRUE.equals(a.getIsActive())).count();
            long sellersCount = accounts.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsActive()) && a.getRole() != null && a.getRole() == RoleEnum.Seller)
                    .count();
            long investorsCount = accounts.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsActive()) && a.getRole() != null && a.getRole() == RoleEnum.Investor)
                    .count();
            long staffsCount = accounts.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsActive()) && a.getRole() != null && a.getRole() == RoleEnum.Staff)
                    .count();

            List<Listing> listings = listingRepository.findAll();
            if (listings == null) listings = Collections.emptyList();

            long totalListings = listings.size();
            long activeListings = listings.stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                    .count();

            SystemKpisDto kpisDto = new SystemKpisDto(
                    totalAccounts,
                    sellersCount,
                    investorsCount,
                    staffsCount,
                    totalListings,
                    activeListings
            );

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(kpisDto, "Get system KPIs successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getRevenueAnalytics(String timeframe) {
        try {
            List<Transaction> transactions = transactionRepository.findAll();
            if (transactions == null) transactions = Collections.emptyList();

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startDate;

            if ("this_month".equalsIgnoreCase(timeframe)) {
                startDate = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            } else if ("this_year".equalsIgnoreCase(timeframe)) {
                startDate = now.withDayOfYear(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            } else {
                startDate = now.minusDays(30);
            }

            List<Transaction> filteredTx = transactions.stream()
                    .filter(t -> "SUCCESS".equalsIgnoreCase(t.getTransactionStatus()) || "PAID".equalsIgnoreCase(t.getTransactionStatus()))
                    .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(startDate))
                    .collect(Collectors.toList());

            BigDecimal postingRevenue = filteredTx.stream()
                    .filter(t -> t.getTransactionType() == TransactionTypeEnum.POSTING_PACKAGE_PAYMENT)
                    .map(t -> BigDecimal.valueOf(t.getTotalAmount() != null ? t.getTotalAmount() : 0L))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal membershipRevenue = filteredTx.stream()
                    .filter(t -> t.getTransactionType() == TransactionTypeEnum.MEMBERSHIP_PAYMENT)
                    .map(t -> BigDecimal.valueOf(t.getTotalAmount() != null ? t.getTotalAmount() : 0L))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalRevenue = postingRevenue.add(membershipRevenue);

            Map<String, BigDecimal> chartMap = new TreeMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Transaction t : filteredTx) {
                if (t.getTransactionType() == TransactionTypeEnum.POSTING_PACKAGE_PAYMENT || t.getTransactionType() == TransactionTypeEnum.MEMBERSHIP_PAYMENT) {
                    String dateKey = t.getCreatedAt().format(formatter);
                    BigDecimal amount = BigDecimal.valueOf(t.getTotalAmount() != null ? t.getTotalAmount() : 0L);
                    chartMap.put(dateKey, chartMap.getOrDefault(dateKey, BigDecimal.ZERO).add(amount));
                }
            }

            List<RevenueChartDataDto> chartData = chartMap.entrySet().stream()
                    .map(e -> new RevenueChartDataDto(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            RevenueBreakdownDto breakdown = new RevenueBreakdownDto(postingRevenue, membershipRevenue);
            RevenueAnalyticsDto responseDto = new RevenueAnalyticsDto(totalRevenue, "VND", breakdown, chartData);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(responseDto, "Get revenue analytics successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getPendingWithdrawals(Integer limit) {
        try {
            List<WalletWithdrawal> withdrawals = walletWithdrawalRepository.findAll();
            if (withdrawals == null) withdrawals = Collections.emptyList();

            List<WalletWithdrawal> pendingList = withdrawals.stream()
                    .filter(w -> w.getStatus() != null && "PENDING".equalsIgnoreCase(w.getStatus()))
                    .sorted(Comparator.comparing(WalletWithdrawal::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            BigDecimal totalPendingAmount = pendingList.stream()
                    .map(w -> w.getAmount() != null ? w.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<WithdrawalRequestDto> requests = pendingList.stream()
                    .limit(limit != null && limit > 0 ? limit : Long.MAX_VALUE)
                    .map(w -> {
                        WithdrawalRequestDto dto = new WithdrawalRequestDto();
                        dto.setWithdrawalId(w.getWalletWithdrawalId());

                        if (w.getWallet() != null && w.getWallet().getAccount() != null) {
                            Account account = w.getWallet().getAccount();
                            dto.setAccountId(account.getAccountId());
                            dto.setAccountName(account.getUsername());
                        }

                        dto.setAmount(w.getAmount());
                        dto.setBankName(w.getBankName());
                        dto.setAccountNumber(w.getBankAccountNumber());
                        dto.setCreatedAt(w.getCreatedAt());
                        return dto;
                    })
                    .collect(Collectors.toList());

            PendingWithdrawalsDto responseDto = new PendingWithdrawalsDto(totalPendingAmount, requests);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(responseDto, "Get pending withdrawals successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}