package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.PostingPackageOrderStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.UserEventTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PostingPackageOrderDtoV2;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.TransactionSummaryDto;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.WalletSummaryDto;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.SellerDashboardServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
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

    @Autowired
    private AuthenUntil authenUntil;

    @Autowired
    private SellerRepository sellerRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ActiveLogRepository activeLogRepository;

    @Autowired
    private ListingMapper listingMapper;

    // SỬA (fix bug NGHIÊM TRỌNG): trước đây hardcode "return 88" — MỌI Seller
    // gọi Dashboard đều thấy dữ liệu của TÀI KHOẢN 88, không phải của chính
    // họ (giống lỗi IDOR — lộ dữ liệu người khác). Giờ lấy đúng từ token đăng
    // nhập, đúng pattern AuthenUntil dùng xuyên suốt hệ thống.
    private Integer getCurrentAccountId() {
        Account currentUser = authenUntil.getCurrentUSer();
        return currentUser != null ? currentUser.getAccountId() : null;
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

    // ════════════════════════════════════════════════════════════════════
    // MỚI: GET /dashboard/seller/listings-summary — theo đúng
    // dashboard_api_specification.md mục 5.2. Đếm theo 4 trạng thái duyệt
    // (ListingStatusEnum trên ListingVerification) — ACTIVE gộp cả điều
    // kiện Seller không tự ẩn tin (SellerListingStatusEnum.ACTIVE).
    // ════════════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getListingsSummary() {
        try {
            Integer currentAccountId = getCurrentAccountId();
            if (currentAccountId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Vui lòng đăng nhập"));
            }
            Seller seller = sellerRepository.findByAccount_AccountId(currentAccountId).orElse(null);
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Seller profile không tồn tại"));
            }

            List<Listing> listings = listingRepository.findBySellerId(seller.getSellerId(),
                    org.springframework.data.domain.Pageable.unpaged()).getContent();

            long activeCount = 0, pendingCount = 0, rejectedCount = 0, expiredCount = 0;
            for (Listing l : listings) {
                ListingVerification lv = l.getListingVerification();
                if (lv == null || lv.getStatus() == null) continue;
                switch (lv.getStatus()) {
                    case APPROVED -> {
                        if (Boolean.TRUE.equals(l.getIsActive())) activeCount++;
                    }
                    case PENDING, WAITING_PAYMENT -> pendingCount++;
                    case REJECTED -> rejectedCount++;
                    case EXPIRED -> expiredCount++;
                }
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("activeCount", activeCount);
            summary.put("pendingCount", pendingCount);
            summary.put("rejectedCount", rejectedCount);
            summary.put("expiredCount", expiredCount);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(summary, "Get listings summary successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // MỚI: GET /dashboard/seller/top-listings — theo đúng
    // dashboard_api_specification.md mục 5.3. Top N tin ACTIVE có viewCount
    // THẬT (đếm từ ActiveLog, không dùng cột Listing.viewCount cũ chưa từng
    // được cập nhật — xem lý do đã ghi rõ ở ListingMapper).
    // ════════════════════════════════════════════════════════════════════
    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getTopListings(Integer limit) {
        try {
            Integer currentAccountId = getCurrentAccountId();
            if (currentAccountId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Vui lòng đăng nhập"));
            }
            Seller seller = sellerRepository.findByAccount_AccountId(currentAccountId).orElse(null);
            if (seller == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Seller profile không tồn tại"));
            }
            int effectiveLimit = (limit != null && limit > 0) ? limit : 5;

            List<Listing> activeListings = listingRepository.findBySellerId(seller.getSellerId(),
                            org.springframework.data.domain.Pageable.unpaged()).getContent()
                    .stream()
                    .filter(l -> Boolean.TRUE.equals(l.getIsActive()))
                    .toList();

            List<Integer> listingIds = activeListings.stream().map(Listing::getListingId).toList();
            Map<Integer, Long> viewCountByListing = new HashMap<>();
            if (!listingIds.isEmpty()) {
                activeLogRepository.countGroupedByListingId(listingIds, UserEventTypeEnum.VIEW)
                        .forEach(p -> viewCountByListing.put(p.getListingId(), p.getViewCount()));
            }

            List<Map<String, Object>> topListings = activeListings.stream()
                    .sorted((a, b) -> Long.compare(
                            viewCountByListing.getOrDefault(b.getListingId(), 0L),
                            viewCountByListing.getOrDefault(a.getListingId(), 0L)))
                    .limit(effectiveLimit)
                    .map(l -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("listingId", l.getListingId());
                        item.put("title", l.getTitle());
                        item.put("thumbnailUrl", listingMapper.resolveThumbnailUrl(l));
                        item.put("price", l.getPrice());
                        item.put("viewCount", viewCountByListing.getOrDefault(l.getListingId(), 0L));
                        return item;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(topListings, "Get top listings successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}