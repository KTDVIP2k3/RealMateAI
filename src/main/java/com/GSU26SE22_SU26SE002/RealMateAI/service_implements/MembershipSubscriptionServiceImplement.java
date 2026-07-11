package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.TransactionTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.MembershipSubscriptionDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.MembershipSubscriptionServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.geolatte.geom.M;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public ResponseEntity<ApiResponse> getMembershipSubscriptions() {
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
                            new MembershipSubscriptionDTO(membershipSubscriptions.getMembershipSubscriptionId()
                                    , membershipSubscriptions.getMembershipSubscriptionEnum_status()
                                    , membershipSubscriptions.getPrice_pay()
                                    , membershipSubscriptions.getQuantity_using()
                                    , membershipSubscriptions.getIsActive()))
                    .toList();
            return  ResponseEntity.status(HttpStatus.OK).body
                    (ApiResponse.success(membershipSubscriptionDTOList, "MembershipSubscription list"));
        } catch (Exception e) {
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

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Renewal membership subscription successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), e.getMessage()));
        }
    }
}

