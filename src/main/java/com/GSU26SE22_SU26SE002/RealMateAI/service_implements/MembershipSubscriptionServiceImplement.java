package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.NotificationTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.MembershipSubscriptionDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.MembershipSubscriptionServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NotificationService;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.geolatte.geom.M;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MembershipSubscriptionServiceImplement implements MembershipSubscriptionServiceInterface {
    @Autowired
    private MembershipSubscriptionRepository membershipSubscriptionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AuthenUntil authenUntil;

    @Autowired
    private InvestorRepository investorRepository;

    @Autowired
    private MembershipPlanRepository membershipPlanRepository;

    @Autowired
    private NotificationService notificationService;

    @Override
    public ResponseEntity<ApiResponse> getMembershipSubscriptions(int page, int size) {
        try{
            Account account = authenUntil.getCurrentUSer();
            if(account == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Account does not exists"));
            }

            if(account.getInvestor() == null){
                Investor investor = new Investor();
                investor.setIsActive(true);
                investor.setCreatedAt(LocalDateTime.now());
                investor.setAccount(account);
                investorRepository.save(investor);
            }

            if(account.getInvestor().getMembershipSubscriptions().isEmpty()
                    || account.getInvestor().getMembershipSubscriptions() == null){
                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "List is empty"));
            }

            List<MembershipSubscriptionDTO> membershipSubscriptionDTOList = account.getInvestor().getMembershipSubscriptions()
                    .stream()
                    .map(membershipSubscriptions ->
                            new MembershipSubscriptionDTO(
                                    membershipSubscriptions.getMembershipSubscriptionId()
                                    , membershipSubscriptions.getMembershipPlan().getMembershipPlanId()
                                    , membershipSubscriptions.getMembershipPlan().getName()
                                    , membershipSubscriptions.getMembershipSubscriptionEnum_status()
                                    , membershipSubscriptions.getPrice_pay()
                                    , membershipSubscriptions.getQuantity_using()
                                    , membershipSubscriptions.getIsActive()))
                    .toList();
            List<MembershipSubscriptionDTO> sortedList = membershipSubscriptionDTOList.stream()
                    .sorted(Comparator.comparing(
                            MembershipSubscriptionDTO::getMembershipSubscriptionId,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .toList();

            boolean isGetAll = (page == 0 && size == 0);

            List<MembershipSubscriptionDTO> pagedContent;
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

                org.springframework.data.domain.Pageable pageable =
                        org.springframework.data.domain.PageRequest.of(effectivePage, effectiveSize);

                pagedContent = sortedList.stream()
                        .skip(pageable.getOffset())
                        .limit(pageable.getPageSize())
                        .collect(Collectors.toList());

                org.springframework.data.domain.Page<MembershipSubscriptionDTO> subscriptionPage =
                        new org.springframework.data.domain.PageImpl<>(pagedContent, pageable, totalElements);

                effectivePage = subscriptionPage.getNumber();
                effectiveSize = subscriptionPage.getSize();
                totalPages = subscriptionPage.getTotalPages();
                isLast = subscriptionPage.isLast();
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", pagedContent);
            result.put("page", effectivePage);
            result.put("size", effectiveSize);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("last", isLast);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(result, "MembershipSubscription list"));    } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> payMemberShipSubscriptions(Integer membershipPlanId) {
        try {
            MembershipPlan membershipPlan = membershipPlanRepository.findById(membershipPlanId).orElse(null);
            if (membershipPlan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Membership plan does not exist"));
            }

            Account account = authenUntil.getCurrentUSer();
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Account does not exists"));
            }

            Investor investor = account.getInvestor();
            if (investor == null) {
                investor = new Investor();
                investor.setIsActive(true);
                investor.setCreatedAt(LocalDateTime.now());
                investor.setAccount(account);
                investor = investorRepository.save(investor);
                account.setInvestor(investor);
            }

            Wallet wallet = walletRepository.findByAccount_AccountId(account.getAccountId()).orElse(null);
            if (wallet == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "You don't have wallet. Please deposit wallet to pay membership plan"));
            }

            BigDecimal price = membershipPlan.getPrice();
            if (wallet.getBalance().compareTo(price) < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Insufficient wallet balance"));
            }

            wallet.setBalance(wallet.getBalance().subtract(price));
            wallet.setUpdatedAt(LocalDateTime.now());

            MembershipSubscription membershipSubscription = new MembershipSubscription();
            membershipSubscription.setMembershipPlan(membershipPlan);
            membershipSubscription.setInvestor(investor);
            membershipSubscription.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.Pending);
            membershipSubscription.setPrice_pay(price);
            membershipSubscription.setQuantity_using(membershipPlan.getQuantity());
            membershipSubscription.setIsActive(true);
            membershipSubscription.setCreatedAt(LocalDateTime.now());

            Transaction transaction = Transaction.builder()
                    .wallet(wallet)
                    .account(account)
                    .transactionType(TransactionTypeEnum.MEMBERSHIP_PAYMENT)
                    .transactionDate(LocalDateTime.now())
                    .totalAmount(price.longValue())
                    .transactionCode("PAY_" + System.currentTimeMillis())
                    .contentDescription("Thanh toán gói thành viên RealMateAI")
                    .transactionStatus("SUCCESS")
                    .createdAt(LocalDateTime.now())
                    .build();

            transactionRepository.save(transaction);
            walletRepository.save(wallet);
            membershipSubscriptionRepository.save(membershipSubscription);

            // MỚI: theo mục 10 (Trung bình) trong RealMateAI_API_Notification_Report.
            notificationService.notify(account,
                    "Mua gói thành viên " + membershipPlan.getName() + " thành công.",
                    NotificationTypeEnum.TRANSACTION);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Pay membership successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> renewMemberShipSubscriptions(Integer membershipSubscriptionId) {
        try {
            MembershipSubscription oldSubscription = membershipSubscriptionRepository.findById(membershipSubscriptionId).orElse(null);
            if (oldSubscription == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Membership subscription id does not exist"));
            }

            MembershipPlan membershipPlan = oldSubscription.getMembershipPlan();
            if (membershipPlan == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Membership plan associated with this subscription does not exist"));
            }

            Account account = authenUntil.getCurrentUSer();
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Account does not exist"));
            }

            Wallet wallet = walletRepository.findByAccount_AccountId(account.getAccountId()).orElse(null);
            if (wallet == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Wallet does not exist, please deposit it"));
            }

            BigDecimal price = membershipPlan.getPrice();
            if (wallet.getBalance().compareTo(price) < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Insufficient wallet balance"));
            }

            wallet.setBalance(wallet.getBalance().subtract(price));
            wallet.setUpdatedAt(LocalDateTime.now());

            oldSubscription.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.Pending);
            oldSubscription.setPrice_pay(price);
            oldSubscription.setQuantity_using(oldSubscription.getQuantity_using() + membershipPlan.getQuantity());
            oldSubscription.setIsActive(true);
            oldSubscription.setUpdatedAt(LocalDateTime.now());

            Transaction transaction = Transaction.builder()
                    .wallet(wallet)
                    .account(account)
                    .transactionType(TransactionTypeEnum.MEMBERSHIP_RENEWAL)
                    .transactionDate(LocalDateTime.now())
                    .totalAmount(price.longValue())
                    .transactionCode("RENEW_" + System.currentTimeMillis())
                    .contentDescription("Gia hạn gói thành viên RealMateAI")
                    .transactionStatus("SUCCESS")
                    .createdAt(LocalDateTime.now())
                    .build();

            transactionRepository.save(transaction);
            walletRepository.save(wallet);
            membershipSubscriptionRepository.save(oldSubscription);

            // MỚI: theo mục 11 (Trung bình) trong RealMateAI_API_Notification_Report.
            notificationService.notify(account,
                    "Gia hạn gói thành viên " + membershipPlan.getName() + " thành công.",
                    NotificationTypeEnum.TRANSACTION);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Renewal membership subscription successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> cancelMembershipSubscriptions(Integer membershipSubscriptionId) {
        try {
            Account account = authenUntil.getCurrentUSer();
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Account does not exist"));
            }

            Investor investor = account.getInvestor();
            if (investor == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Investor account does not exist"));
            }

            MembershipSubscription subscription = membershipSubscriptionRepository.findById(membershipSubscriptionId).orElse(null);
            if (subscription == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Membership subscription ID does not exist"));
            }

            if (subscription.getInvestor() == null ||
                    !subscription.getInvestor().getInvestorId().equals(investor.getInvestorId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail(HttpStatus.FORBIDDEN.toString(), "You do not have permission to cancel this subscription"));
            }

            MembershipSubscriptionEnum currentStatus = subscription.getMembershipSubscriptionEnum_status();

            if (MembershipSubscriptionEnum.Pending.equals(currentStatus)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Membership subscription is already in PENDING status and cannot be canceled."));
            }

            if (MembershipSubscriptionEnum.OutDated.equals(currentStatus)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Membership subscription is already OUTDATED and cannot be canceled."));
            }

            subscription.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.Pending);
            subscription.setIsActive(false);
            subscription.setUpdatedAt(LocalDateTime.now());

            membershipSubscriptionRepository.save(subscription);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Cancel membership subscription successfully. Status changed to PENDING."));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
    }


    @Override
    @Transactional
    public ResponseEntity<ApiResponse> activeMembershipSubscriptions(Integer membershipSubscriptionId) {
        try {
            Account account = authenUntil.getCurrentUSer();
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Account does not exist"));
            }

            Investor investor = account.getInvestor();
            if (investor == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Investor account does not exist"));
            }

            MembershipSubscription subscription = membershipSubscriptionRepository.findById(membershipSubscriptionId).orElse(null);
            if (subscription == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Membership subscription ID does not exist"));
            }

            if (subscription.getInvestor() == null ||
                    !subscription.getInvestor().getInvestorId().equals(investor.getInvestorId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.fail(HttpStatus.FORBIDDEN.toString(), "You do not have permission to activate this subscription"));
            }

            if (MembershipSubscriptionEnum.Using.equals(subscription.getMembershipSubscriptionEnum_status())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "Membership subscription is already in using status"));
            }

            boolean existsUsingStatus = investor.getMembershipSubscriptions() != null &&
                    investor.getMembershipSubscriptions().stream()
                            .anyMatch(m -> m.getMembershipSubscriptionEnum_status() != null
                                    && m.getMembershipSubscriptionEnum_status().equals(MembershipSubscriptionEnum.Using));

            if (existsUsingStatus) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(HttpStatus.BAD_REQUEST.toString(), "You already have an active membership subscription in USING status. Please expire or cancel it first."));
            }

            subscription.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.Using);
            subscription.setIsActive(true);
            subscription.setUpdatedAt(LocalDateTime.now());

            membershipSubscriptionRepository.save(subscription);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Activate membership subscription successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
    }
}
