package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.PostingPackageOrderRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PostingPackageOrderDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ListingVerificationServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PostingPackageOrderServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
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
    public ResponseEntity<ApiResponse> payPostingPackage(PostingPackageOrderRequest postingPackageOrderRequest) {
        try {
            PostingPackage postingPackage = postingPackageRepository.findById(postingPackageOrderRequest.getPostingPackageId()).orElse(null);
            if (postingPackage == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Posting package id does not exist"));
            }

            Listing listing = listingRepository.findById(postingPackageOrderRequest.getListingId()).orElse(null);
            if (listing == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Listing id does not exist"));
            }

            Account account = listing.getSeller().getAccount();
            int accountId = account.getAccountId();

            Wallet wallet = walletRepository.findByAccount_AccountId(accountId).orElse(null);
            if (wallet == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Wallet of this account does not exist"));
            }

            BigDecimal totalAmount = postingPackageOrderRequest.getTotalAmount();
            if (wallet.getBalance().compareTo(totalAmount) < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Insufficient wallet balance"));
            }
            wallet.setBalance(wallet.getBalance().subtract(totalAmount));
            wallet.setUpdatedAt(LocalDateTime.now());

            LocalDateTime startDate = LocalDateTime.now();
            int durationDays = postingPackageOrderRequest.getDuration();

            PostingPackageOrder postingPackageOrder = new PostingPackageOrder();
            postingPackageOrder.setPostingPackage(postingPackage);
            postingPackageOrder.setListing(listing);
            postingPackageOrder.setDuration(durationDays);
            postingPackageOrder.setTotalAmount(totalAmount);
            postingPackageOrder.setCreatedAt(LocalDateTime.now());

            // SỬA: KHÔNG còn set thẳng isActive/startDate/endDate ở đây — tách
            // riêng ra transitionListingForNewPackageOrder() (xem javadoc bên dưới).
            transitionListingForNewPackageOrder(listing, postingPackageOrder, startDate);

            Transaction transaction = Transaction.builder()
                    .wallet(wallet)
                    .account(account)
                    .transactionType(TransactionTypeEnum.POSTING_PACKAGE_PAYMENT)
                    .transactionDate(LocalDateTime.now())
                    .totalAmount(totalAmount.longValue())
                    .transactionCode("PAY_" + System.currentTimeMillis())
                    .contentDescription("Thanh toán gói dịch vụ đăng tin RealMateAI")
                    .transactionStatus("SUCCESS")
                    .createdAt(LocalDateTime.now())
                    .build();

            transactionRepository.save(transaction);
            walletRepository.save(wallet);
            postingPackageOrderRepository.save(postingPackageOrder);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Payment for posting package successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
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
    }
}