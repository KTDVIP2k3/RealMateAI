package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.NotificationTypeEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Investor;
import com.GSU26SE22_SU26SE002.RealMateAI.model.MembershipPlan;
import com.GSU26SE22_SU26SE002.RealMateAI.model.MembershipSubscription;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Wallet;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.InvestorRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.MembershipPlanRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.MembershipSubscriptionRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.TransactionRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WalletRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NotificationService;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembershipSubscriptionServiceImplement - Membership Subscription")
class MembershipSubscriptionServiceImplementTest {

    @Mock
    private MembershipSubscriptionRepository membershipSubscriptionRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private AuthenUntil authenUntil;
    @Mock
    private InvestorRepository investorRepository;
    @Mock
    private MembershipPlanRepository membershipPlanRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MembershipSubscriptionServiceImplement subscriptionService;

    private Account sampleAccount;
    private Investor sampleInvestor;
    private Wallet sampleWallet;
    private MembershipPlan samplePlan;
    private MembershipSubscription sampleSubscription;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setAccountId(1);

        sampleInvestor = new Investor();
        sampleInvestor.setInvestorId(1);
        sampleAccount.setInvestor(sampleInvestor);
        sampleInvestor.setAccount(sampleAccount);

        sampleWallet = new Wallet();
        sampleWallet.setWalletId(1);
        sampleWallet.setBalance(new BigDecimal("500000"));

        samplePlan = new MembershipPlan();
        samplePlan.setMembershipPlanId(1);
        samplePlan.setName("Pro Plan");
        samplePlan.setPrice(new BigDecimal("100000"));
        samplePlan.setQuantity(100);

        sampleSubscription = new MembershipSubscription();
        sampleSubscription.setMembershipSubscriptionId(1);
        sampleSubscription.setMembershipPlan(samplePlan);
        sampleSubscription.setIsActive(true);
        sampleSubscription.setQuantity_using(10);
    }

    @Nested
    @DisplayName("View Membership Subscriptions")
    class ViewMembershipSubscriptionsTests {

        @Test
        @DisplayName("UC-209, 210: Valid request returns OK")
        void getMembershipSubscriptions_valid_returnsOk() {
            List<MembershipSubscription> subscriptions = new ArrayList<>();
            subscriptions.add(sampleSubscription);
            sampleInvestor.setMembershipSubscriptions(subscriptions);

            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = subscriptionService.getMembershipSubscriptions(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("MembershipSubscription list", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Empty list returns OK")
        void getMembershipSubscriptions_empty_returnsOk() {
            sampleInvestor.setMembershipSubscriptions(new ArrayList<>());
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = subscriptionService.getMembershipSubscriptions(0, 10);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("List is empty", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Unauthenticated returns Not Found")
        void getMembershipSubscriptions_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = subscriptionService.getMembershipSubscriptions(0, 10);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exists", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getMembershipSubscriptions_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = subscriptionService.getMembershipSubscriptions(0, 10);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Subscribe Membership Plan")
    class SubscribeMembershipPlanTests {

        @Test
        @DisplayName("UC-214, 215: Valid request returns OK")
        void payMemberShipSubscriptions_valid_returnsOk() {
            when(membershipPlanRepository.findById(1)).thenReturn(Optional.of(samplePlan));
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(sampleWallet));
            when(transactionRepository.save(any())).thenReturn(null);
            when(walletRepository.save(any())).thenReturn(sampleWallet);
            when(membershipSubscriptionRepository.save(any())).thenReturn(sampleSubscription);

            ResponseEntity<ApiResponse> response = subscriptionService.payMemberShipSubscriptions(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Pay membership successfully", response.getBody().getMessage());
            verify(notificationService).notify(eq(sampleAccount), anyString(), eq(NotificationTypeEnum.TRANSACTION));
        }

        @Test
        @DisplayName("Non-existent plan returns NOT_FOUND")
        void payMemberShipSubscriptions_notFound_returnsNotFound() {
            when(membershipPlanRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = subscriptionService.payMemberShipSubscriptions(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Tham số rỗng/bằng 0 trả về NOT_FOUND hoặc BAD_REQUEST")
        void payMemberShipSubscriptions_emptyId_returnsNotFound() {
            when(membershipPlanRepository.findById(0)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = subscriptionService.payMemberShipSubscriptions(0);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Unauthenticated returns Not Found")
        void payMemberShipSubscriptions_unauthenticated_returnsUnauthorized() {
            when(membershipPlanRepository.findById(1)).thenReturn(Optional.of(samplePlan));
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = subscriptionService.payMemberShipSubscriptions(1);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exists", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Wallet does not exist returns BAD_REQUEST")
        void payMemberShipSubscriptions_noWallet_returnsBadRequest() {
            when(membershipPlanRepository.findById(1)).thenReturn(Optional.of(samplePlan));
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = subscriptionService.payMemberShipSubscriptions(1);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("You don't have wallet. Please deposit wallet to pay membership plan", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Insufficient balance returns BAD_REQUEST")
        void payMemberShipSubscriptions_insufficientBalance_returnsBadRequest() {
            sampleWallet.setBalance(new BigDecimal("50000")); // Plan is 100000
            when(membershipPlanRepository.findById(1)).thenReturn(Optional.of(samplePlan));
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(sampleWallet));

            ResponseEntity<ApiResponse> response = subscriptionService.payMemberShipSubscriptions(1);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Insufficient wallet balance", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void payMemberShipSubscriptions_exception_returnsServerError() {
            when(membershipPlanRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = subscriptionService.payMemberShipSubscriptions(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Renew Membership Subscription")
    class RenewMembershipSubscriptionTests {

        @Test
        @DisplayName("Valid request returns OK")
        void renewMemberShipSubscriptions_valid_returnsOk() {
            when(membershipSubscriptionRepository.findById(1)).thenReturn(Optional.of(sampleSubscription));
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(sampleWallet));
            
            ResponseEntity<ApiResponse> response = subscriptionService.renewMemberShipSubscriptions(1);
            
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Renewal membership subscription successfully", response.getBody().getMessage());
            verify(notificationService).notify(eq(sampleAccount), anyString(), eq(NotificationTypeEnum.TRANSACTION));
        }

        @Test
        @DisplayName("Non-existent subscription returns Not_Found")
        void renewMemberShipSubscriptions_notFoundSub_returnsNotFound() {
            when(membershipSubscriptionRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = subscriptionService.renewMemberShipSubscriptions(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Unauthenticated returns Not Found")
        void renewMemberShipSubscriptions_unauth_returnsUnauthorized() {
            when(membershipSubscriptionRepository.findById(1)).thenReturn(Optional.of(sampleSubscription));
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = subscriptionService.renewMemberShipSubscriptions(1);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Insufficient balance returns BAD_REQUEST")
        void renewMemberShipSubscriptions_insufficientBalance_returnsBadRequest() {
            sampleWallet.setBalance(new BigDecimal("50000"));
            when(membershipSubscriptionRepository.findById(1)).thenReturn(Optional.of(sampleSubscription));
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(walletRepository.findByAccount_AccountId(1)).thenReturn(Optional.of(sampleWallet));

            ResponseEntity<ApiResponse> response = subscriptionService.renewMemberShipSubscriptions(1);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Insufficient wallet balance", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void renewMemberShipSubscriptions_exception_returnsServerError() {
            when(membershipSubscriptionRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = subscriptionService.renewMemberShipSubscriptions(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Cancel Membership Subscription")
    class CancelMembershipSubscriptionTests {

        @Test
        @DisplayName("Valid request returns OK")
        void cancelMembershipSubscriptions_valid_returnsOk() {
            sampleSubscription.setInvestor(sampleInvestor);
            sampleSubscription.setMembershipSubscriptionEnum_status(com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum.Using);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(membershipSubscriptionRepository.findById(1)).thenReturn(Optional.of(sampleSubscription));

            ResponseEntity<ApiResponse> response = subscriptionService.cancelMembershipSubscriptions(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Cancel membership subscription successfully. Status changed to PENDING.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Already Pending returns BAD_REQUEST")
        void cancelMembershipSubscriptions_alreadyPending_returnsBadRequest() {
            sampleSubscription.setInvestor(sampleInvestor);
            sampleSubscription.setMembershipSubscriptionEnum_status(com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum.Pending);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(membershipSubscriptionRepository.findById(1)).thenReturn(Optional.of(sampleSubscription));

            ResponseEntity<ApiResponse> response = subscriptionService.cancelMembershipSubscriptions(1);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Membership subscription is already in PENDING status and cannot be canceled.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Already Outdated returns BAD_REQUEST")
        void cancelMembershipSubscriptions_alreadyOutdated_returnsBadRequest() {
            sampleSubscription.setInvestor(sampleInvestor);
            sampleSubscription.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.OutDated);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(membershipSubscriptionRepository.findById(1)).thenReturn(Optional.of(sampleSubscription));

            ResponseEntity<ApiResponse> response = subscriptionService.cancelMembershipSubscriptions(1);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Membership subscription is already OUTDATED and cannot be canceled.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Unauthenticated returns Not_Found")
        void cancelMembershipSubscriptions_unauth_returnsUnauthorized() {
            lenient().when(membershipSubscriptionRepository.findById(1)).thenReturn(Optional.of(sampleSubscription));
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = subscriptionService.cancelMembershipSubscriptions(1);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent subscription returns NOT_FOUND")
        void cancelMembershipSubscriptions_notFound_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(membershipSubscriptionRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = subscriptionService.cancelMembershipSubscriptions(99);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Membership subscription ID does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void cancelMembershipSubscriptions_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(membershipSubscriptionRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = subscriptionService.cancelMembershipSubscriptions(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Activate Membership Subscription")
    class ActivateMembershipSubscriptionTests {

        @Test
        @DisplayName("Valid request returns OK")
        void activeMembershipSubscriptions_valid_returnsOk() {
            sampleSubscription.setInvestor(sampleInvestor);
            sampleSubscription.setMembershipSubscriptionEnum_status(com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum.Pending);
            sampleInvestor.setMembershipSubscriptions(new ArrayList<>());
            
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(membershipSubscriptionRepository.findById(1)).thenReturn(Optional.of(sampleSubscription));

            ResponseEntity<ApiResponse> response = subscriptionService.activeMembershipSubscriptions(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Activate membership subscription successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Already Using returns BAD_REQUEST")
        void activeMembershipSubscriptions_alreadyUsing_returnsBadRequest() {
            sampleSubscription.setInvestor(sampleInvestor);
            sampleSubscription.setMembershipSubscriptionEnum_status(com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum.Using);
            
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(membershipSubscriptionRepository.findById(1)).thenReturn(Optional.of(sampleSubscription));

            ResponseEntity<ApiResponse> response = subscriptionService.activeMembershipSubscriptions(1);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Membership subscription is already in using status", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Unauthenticated returns Not Found")
        void activeMembershipSubscriptions_unauth_returnsUnauthorized() {
            lenient().when(membershipSubscriptionRepository.findById(1)).thenReturn(Optional.of(sampleSubscription));
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = subscriptionService.activeMembershipSubscriptions(1);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent subscription returns NOT_FOUND")
        void activeMembershipSubscriptions_notFound_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(membershipSubscriptionRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = subscriptionService.activeMembershipSubscriptions(99);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Membership subscription ID does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void activeMembershipSubscriptions_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(membershipSubscriptionRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = subscriptionService.activeMembershipSubscriptions(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }
}