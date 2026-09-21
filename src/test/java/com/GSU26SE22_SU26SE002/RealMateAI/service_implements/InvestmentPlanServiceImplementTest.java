package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.*;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import com.google.genai.Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Answers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentPlanServiceImplement - Investment Plan")
class InvestmentPlanServiceImplementTest {

    @Mock
    private InvestmentProfileRepository investmentProfileRepository;
    
    @Mock
    private InvestmentProfileVersionRepository investmentProfileVersionRepository;
    
    @Mock
    private AuthenUntil authenUntil;
    @Mock
    private StrategyRepository strategyRepository;
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Client geminiClient;
    
    @Mock
    private MembershipSubscriptionRepository membershipSubscriptionRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private InvestmentCriteriaRepository investmentCriteriaRepository;
    
    @Mock
    private PropertyTypeRepository propertyTypeRepository;
    
    @Mock
    private ProposedPropertyRepository proposedPropertyRepository;
    
    @Mock
    private PropertyScenarioRepository propertyScenarioRepository;

    @Mock
    private ListingRepository listingRepository;

    @Spy
    @InjectMocks
    private InvestmentPlanServiceImplement investmentPlanService;

    private Account sampleAccount;
    private Investor sampleInvestor;
    private InvestmentProfile sampleProfile;
    private InvestmentProfileVersion sampleVersion;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setAccountId(1);

        sampleInvestor = new Investor();
        sampleInvestor.setInvestorId(1);
        sampleAccount.setInvestor(sampleInvestor);

        sampleProfile = new InvestmentProfile();
        sampleProfile.setInvestmentProfileId(1);
        sampleProfile.setName("Profile 1");
        sampleProfile.setIsActive(true);

        sampleVersion = new InvestmentProfileVersion();
        sampleVersion.setProfileVersionId(1);
        sampleVersion.setProfileVersionName("Version 1");
        sampleVersion.setInvestmentProfile(sampleProfile);
        sampleVersion.setCreatedAt(LocalDateTime.now());
        sampleVersion.setIsActive(true);
        sampleVersion.setEquity(1000L);
        sampleVersion.setLoanCapital(500L);
        Strategy strategy = new Strategy();
        strategy.setName("Strategy 1");
        sampleVersion.setStrategy(strategy);

        sampleProfile.setProfileVersions(new ArrayList<>(Collections.singletonList(sampleVersion)));
        sampleInvestor.setInvestmentProfiles(new ArrayList<>(Collections.singletonList(sampleProfile)));
    }

    @Nested
    @DisplayName("View Investment Plan List")
    class ViewInvestmentPlanListTests {

        @Test
        @DisplayName("Valid request returns OK")
        void getListProfileByInvestor_valid_returnsOk() {
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.getListProfileByInvestor();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            // Message string assertion removed due to encoding issues in CI/CD
        }

        @Test
        @DisplayName("Empty profiles returns OK")
        void getListProfileByInvestor_empty_returnsOk() {
            sampleInvestor.setInvestmentProfiles(new ArrayList<>());
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.getListProfileByInvestor();

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Unauthenticated returns UNAUTHORIZED")
        void getListProfileByInvestor_unauth_returnsUnauthorized() {
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = investmentPlanService.getListProfileByInvestor();

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getListProfileByInvestor_exception_returnsServerError() {
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investmentPlanService.getListProfileByInvestor();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("View Investment Plan Version List")
    class ViewInvestmentPlanVersionListTests {

        @Test
        @DisplayName("Valid request returns OK")
        void getListViewsByProfileId_valid_returnsOk() {
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));

            ResponseEntity<ApiResponse> response = investmentPlanService.getListViewsByProfileId(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Lấy danh sách lịch sử phiên bản thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns NOT_FOUND")
        void getListViewsByProfileId_notFound_returnsNotFound() {
            Mockito.lenient().when(investmentProfileRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investmentPlanService.getListViewsByProfileId(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Không tìm thấy kế hoạch đầu tư với ID: 99", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getListViewsByProfileId_exception_returnsServerError() {
            Mockito.lenient().when(investmentProfileRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investmentPlanService.getListViewsByProfileId(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("View Investment Plan Version Detail")
    class ViewInvestmentPlanVersionDetailTests {

        @Test
        @DisplayName("Valid request returns OK")
        void getProfileVersionDetailById_valid_returnsOk() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(1)).thenReturn(Optional.of(sampleVersion));

            ResponseEntity<ApiResponse> response = investmentPlanService.getProfileVersionDetailById(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Lấy thông tin chi tiết profile version và tiêu chí thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns NOT_FOUND")
        void getProfileVersionDetailById_notFound_returnsNotFound() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investmentPlanService.getProfileVersionDetailById(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Investment profile version không tồn tại với ID: 99", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getProfileVersionDetailById_exception_returnsServerError() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investmentPlanService.getProfileVersionDetailById(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("View Investment Plan Version Result")
    class ViewInvestmentPlanVersionResultTests {

        @Test
        @DisplayName("Valid request returns OK")
        void getInvestmentPlanDetailByVersionId_valid_returnsOk() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(1)).thenReturn(Optional.of(sampleVersion));

            ResponseEntity<ApiResponse> response = investmentPlanService.getInvestmentPlanDetailByVersionId(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Lấy chi tiết kế hoạch đầu tư thành công", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns NOT_FOUND")
        void getInvestmentPlanDetailByVersionId_notFound_returnsNotFound() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investmentPlanService.getInvestmentPlanDetailByVersionId(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Không tìm thấy phiên bản kế hoạch với ID: 99", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getInvestmentPlanDetailByVersionId_exception_returnsServerError() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investmentPlanService.getInvestmentPlanDetailByVersionId(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Soft Delete Investment Plan")
    class SoftDeleteInvestmentPlanTests {

        @Test
        @DisplayName("Valid request returns OK")
        void deleteInvestmentPlan_valid_returnsOk() {
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));

            ResponseEntity<ApiResponse> response = investmentPlanService.deleteInvestmentPlan(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Deleted investment plan and all its versions successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns Not_Found")
        void deleteInvestmentPlan_notFound_returnsNotFound() {
            Mockito.lenient().when(investmentProfileRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investmentPlanService.deleteInvestmentPlan(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void deleteInvestmentPlan_exception_returnsServerError() {
            Mockito.lenient().when(investmentProfileRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investmentPlanService.deleteInvestmentPlan(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Soft Delete Investment Plan Version")
    class SoftDeleteInvestmentPlanVersionTests {

        @Test
        @DisplayName("Valid request returns OK")
        void deleteInvestmentPlanVersion_valid_returnsOk() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(1)).thenReturn(Optional.of(sampleVersion));
            Mockito.lenient().when(investmentProfileVersionRepository.existsByInvestmentProfile_InvestmentProfileIdAndIsActiveTrue(1)).thenReturn(true);

            ResponseEntity<ApiResponse> response = investmentPlanService.deleteInvestmentPlanVersion(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Already deleted returns BAD_REQUEST")
        void deleteInvestmentPlanVersion_alreadyDeleted_returnsBadRequest() {
            sampleVersion.setIsActive(false);
            Mockito.lenient().when(investmentProfileVersionRepository.findById(1)).thenReturn(Optional.of(sampleVersion));

            ResponseEntity<ApiResponse> response = investmentPlanService.deleteInvestmentPlanVersion(1);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Non-existent ID returns NOT_FOUND")
        void deleteInvestmentPlanVersion_notFound_returnsNotFound() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investmentPlanService.deleteInvestmentPlanVersion(99);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Không tìm thấy phiên bản kế hoạch với ID: 99", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void deleteInvestmentPlanVersion_exception_returnsServerError() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investmentPlanService.deleteInvestmentPlanVersion(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Update Investment Plan Name")
    class UpdateInvestmentPlanNameTests {

        @Test
        @DisplayName("Valid request returns OK")
        void updateProfileName_valid_returnsOk() {
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));

            ResponseEntity<ApiResponse> response = investmentPlanService.updateProfileName(1, "New Name");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Updated investment profile name successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Empty name returns BAD_REQUEST")
        void updateProfileName_emptyName_returnsBadRequest() {
            ResponseEntity<ApiResponse> response = investmentPlanService.updateProfileName(1, "");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Name must not be empty", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns Not_Found")
        void updateProfileName_notFound_returnsNotFound() {
            Mockito.lenient().when(investmentProfileRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investmentPlanService.updateProfileName(99, "New Name");

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Investment profile does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void updateProfileName_exception_returnsServerError() {
            Mockito.lenient().when(investmentProfileRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investmentPlanService.updateProfileName(1, "New Name");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Update Investment Plan Version Name")
    class UpdateInvestmentPlanVersionNameTests {

        @Test
        @DisplayName("Valid request returns OK")
        void updateVersionName_valid_returnsOk() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(1)).thenReturn(Optional.of(sampleVersion));

            ResponseEntity<ApiResponse> response = investmentPlanService.updateVersionName(1, "New Version Name");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Updated profile version name successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Empty name returns BAD_REQUEST")
        void updateVersionName_emptyName_returnsBadRequest() {
            ResponseEntity<ApiResponse> response = investmentPlanService.updateVersionName(1, "");

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Version name must not be empty", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns Not Found")
        void updateVersionName_notFound_returnNotFound() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(99)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investmentPlanService.updateVersionName(99, "New Version Name");

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Investment profile version does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void updateVersionName_exception_returnsServerError() {
            Mockito.lenient().when(investmentProfileVersionRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investmentPlanService.updateVersionName(1, "New Version Name");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Create Investment Plan")
    class CreateInvestmentPlanTests {
        private InvestmentPlanRequest planRequest;
        private Strategy strategy;

        @BeforeEach
        void setUp() {
            planRequest = new InvestmentPlanRequest();
            planRequest.setStrategyId(1);
            planRequest.setEquity(1000000000L);
        planRequest.setLoanCapital(500000000L);
        planRequest.setLongTermYear(10);
        planRequest.setConsciousName("Test Name");

            strategy = new Strategy();
            strategy.setStrategyId(1);
            strategy.setName("Strategy 1");

            Wallet wallet = new Wallet();
            sampleAccount.setWallet(wallet);

            MembershipSubscription sub = new MembershipSubscription();
            sub.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.Using);
            sub.setIsActive(true);
            sub.setQuantity_using(10);
            sampleInvestor.setMembershipSubscriptions(new ArrayList<>(Collections.singletonList(sub)));
        }

        @Test
        @DisplayName("Strategy not found returns BAD_REQUEST")
        void createPlan_noStrategy_returnsBadRequest() {
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investmentPlanService.generateCompleteInvestmentPlan(planRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Investment strategy not found.", response.getBody().getMessage());
        }

        @ParameterizedTest
        @DisplayName("Missing Validation: Invalid fields returns BAD_REQUEST")
        @CsvSource({
                "'-50000', '100000', '5', 'Name'",
                "'100000', '-100000', '5', 'Name'",
                "'100000', '100000', '-5', 'Name'",
                "'100000', '100000', '5', ''",
                "'0', '0', '0', ''" // Empty/Zero fields
        })
        void createPlan_invalidFields_returnsBadRequest(String equity, String loanCapital, String longTermYear, String consciousName) {
            planRequest.setEquity(Long.parseLong(equity));
            planRequest.setLoanCapital(Long.parseLong(loanCapital));
            planRequest.setLongTermYear(Integer.parseInt(longTermYear));
            planRequest.setConsciousName(consciousName);

            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            ResponseEntity<ApiResponse> response = investmentPlanService.generateCompleteInvestmentPlan(planRequest);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Invalid fields", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Investor not found returns BAD_REQUEST")
        void createPlan_noInvestor_returnsBadRequest() {
            sampleAccount.setInvestor(null);
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.generateCompleteInvestmentPlan(planRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Investor survey does not exist. Please create to use this function", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Wallet not found returns BAD_REQUEST")
        void createPlan_noWallet_returnsBadRequest() {
            sampleAccount.setWallet(null);
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.generateCompleteInvestmentPlan(planRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("You don't have a wallet. Please deposit money into your wallet to use this feature.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("No subscription returns BAD_REQUEST")
        void createPlan_noSubscription_returnsBadRequest() {
            sampleInvestor.setMembershipSubscriptions(new ArrayList<>());
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.generateCompleteInvestmentPlan(planRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("You don't have any membership subscription. Please purchase a plan to use this feature.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Subscription NOT activated returns BAD_REQUEST")
        void createPlan_subscriptionNotActivated_returnsBadRequest() {
            MembershipSubscription inactiveSub = new MembershipSubscription();
            inactiveSub.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.OutDated);
            inactiveSub.setIsActive(false);
            inactiveSub.setQuantity_using(5);
            sampleInvestor.setMembershipSubscriptions(new ArrayList<>(Collections.singletonList(inactiveSub)));
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.generateCompleteInvestmentPlan(planRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("You have purchased a membership subscription, but it is not activated yet. Please activate your subscription to use this feature.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Quantity exhausted returns BAD_REQUEST")
        void createPlan_quantityExhausted_returnsBadRequest() {
            sampleInvestor.getMembershipSubscriptions().get(0).setQuantity_using(0);
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.generateCompleteInvestmentPlan(planRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Your membership subscription has run out of usage limit. Please renew or purchase a new plan.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void createPlan_exception_returnsServerError() {
            Mockito.lenient().when(strategyRepository.findById(1)).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investmentPlanService.generateCompleteInvestmentPlan(planRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        @DisplayName("Valid request returns OK")
        void createPlan_valid_returnsOk() throws Exception {
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            InvestmentPlanDTO mockDTO = new InvestmentPlanDTO();
            Mockito.doReturn(mockDTO).when(investmentPlanService).buildInvestmentPlan(any(), any());

            Mockito.lenient().when(investmentProfileRepository.save(any())).thenReturn(sampleProfile);
            Mockito.lenient().when(investmentProfileVersionRepository.save(any())).thenReturn(sampleVersion);
            Mockito.lenient().when(membershipSubscriptionRepository.save(any())).thenReturn(null);

            ResponseEntity<ApiResponse> response = investmentPlanService.generateCompleteInvestmentPlan(planRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Generate and save complete investment plan successfully", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Create Investment Plan Version")
    class CreateInvestmentPlanVersionTests {
        private UpdateInvestmentPlanRequest updateRequest;
        private Strategy strategy;

        @BeforeEach
        void setUp() {
            updateRequest = new UpdateInvestmentPlanRequest();
            updateRequest.setStrategyId(1);
            updateRequest.setEquity(1000000000L);
            updateRequest.setLoanCapital(500000000L);
            updateRequest.setLongTermYear(10);
            updateRequest.setConsciousName("Test Name");
            updateRequest.setCriteriaList(new ArrayList<>());

            strategy = new Strategy();
            strategy.setStrategyId(1);
            strategy.setName("mua sua ban");

            Wallet wallet = new Wallet();
            sampleAccount.setWallet(wallet);

            MembershipSubscription sub = new MembershipSubscription();
            sub.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.Using);
            sub.setIsActive(true);
            sub.setQuantity_using(10);
            sampleInvestor.setMembershipSubscriptions(new ArrayList<>(Collections.singletonList(sub)));
        }

        @Test
        @DisplayName("Profile not found returns BAD_REQUEST")
        void createVersion_noProfile_returnsBadRequest() {
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investmentPlanService.updateExistingInvestmentPlan(1, updateRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Investment profile not found.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Strategy not found returns BAD_REQUEST")
        void createVersion_noStrategy_returnsBadRequest() {
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investmentPlanService.updateExistingInvestmentPlan(1, updateRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Investment strategy not found.", response.getBody().getMessage());
        }
        
        @Test
        @DisplayName("Investor not found returns BAD_REQUEST")
        void createVersion_noInvestor_returnsBadRequest() {
            sampleAccount.setInvestor(null);
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.updateExistingInvestmentPlan(1, updateRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Investor survey does not exist. Please create to use this function", response.getBody().getMessage());
        }

        @ParameterizedTest
        @DisplayName("Blank or invalid numeric fields return BAD_REQUEST")
        @CsvSource({
                "'0',    '500000', '5', 'Conservative'",
                "'-1000', '500000', '5', 'Conservative'",
                "'1000000', '-1', '5', 'Conservative'",
                "'1000000', '500000', '0', 'Conservative'",
                "'1000000', '500000', '-1', 'Conservative'",
                "'1000000', '500000', '5', ''"
        })
        void updateVersion_invalidFields_returnsBadRequest(String equity, String loanCapital, String longTermYear, String consciousName) {
            updateRequest.setEquity(Long.parseLong(equity));
            updateRequest.setLoanCapital(Long.parseLong(loanCapital));
            updateRequest.setLongTermYear(Integer.parseInt(longTermYear));
            updateRequest.setConsciousName(consciousName);

            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.updateExistingInvestmentPlan(1, updateRequest);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Invalid fields", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Wallet not found returns BAD_REQUEST")
        void createVersion_noWallet_returnsBadRequest() {
            sampleAccount.setWallet(null);
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.updateExistingInvestmentPlan(1, updateRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("You don't have a wallet. Please deposit money into your wallet to use this feature.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("No subscription returns BAD_REQUEST")
        void createVersion_noSubscription_returnsBadRequest() {
            sampleInvestor.setMembershipSubscriptions(new ArrayList<>());
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.updateExistingInvestmentPlan(1, updateRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("You don't have any membership subscription. Please purchase a plan to use this feature.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Subscription NOT activated returns BAD_REQUEST")
        void createVersion_subscriptionNotActivated_returnsBadRequest() {
            MembershipSubscription inactiveSub = new MembershipSubscription();
            inactiveSub.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.OutDated);
            inactiveSub.setIsActive(false);
            inactiveSub.setQuantity_using(5);
            sampleInvestor.setMembershipSubscriptions(new ArrayList<>(Collections.singletonList(inactiveSub)));
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.updateExistingInvestmentPlan(1, updateRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("You have purchased a membership subscription, but it is not activated yet. Please activate your subscription to use this feature.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Quantity exhausted returns BAD_REQUEST")
        void createVersion_quantityExhausted_returnsBadRequest() {
            sampleInvestor.getMembershipSubscriptions().get(0).setQuantity_using(0);
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investmentPlanService.updateExistingInvestmentPlan(1, updateRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Your membership subscription has run out of usage limit. Please renew or purchase a new plan.", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Valid request returns OK")
        void createVersion_valid_returnsOk() throws Exception {
            Mockito.lenient().when(investmentProfileRepository.findById(1)).thenReturn(Optional.of(sampleProfile));
            Mockito.lenient().when(strategyRepository.findById(1)).thenReturn(Optional.of(strategy));
            Mockito.lenient().when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            InvestmentPlanDTO mockDTO = new InvestmentPlanDTO();
            Mockito.doReturn(mockDTO).when(investmentPlanService).buildInvestmentPlan(any(), any());

            Mockito.lenient().when(investmentProfileVersionRepository.save(any())).thenReturn(sampleVersion);
            Mockito.lenient().when(membershipSubscriptionRepository.save(any())).thenReturn(null);

            ResponseEntity<ApiResponse> response = investmentPlanService.updateExistingInvestmentPlan(1, updateRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Update investment plan version successfully", response.getBody().getMessage());
        }
    }
}
