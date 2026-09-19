package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.GenerateFuturePlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentFuturePlanServiceImplement ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â Future Investment Plan")
class InvestmentFuturePlanServiceImplementTest {

    @Mock private AuthenUntil authenUntil;
    @Mock private InvestmentProfileVersionRepository investmentProfileVersionRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private StrategyRepository strategyRepository;
    @Mock private FutureInvestmentPlanRepository futureInvestmentPlanRepository;
    @Mock private Client geminiClient;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private InvestmentFuturePlanServiceImplement futurePlanService;

    private Account investorAccount;
    private InvestmentProfileVersion sourceVersion;

    @BeforeEach
    void setUp() {
        investorAccount = new Account();
        investorAccount.setAccountId(1);
        Investor investor = Investor.builder().investorId(5).account(investorAccount).build();
        investorAccount.setInvestor(investor);

        sourceVersion = InvestmentProfileVersion.builder()
                .profileVersionId(6)
                .profileVersionName("Ke hoach goc")
                .equity(2_000_000_000L)
                .loanCapital(1_000_000_000L)
                .longTermYear(20)
                .investmentCriterias(Collections.emptyList())
                .build();
    }

    // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 113. Create Future Investment Plan ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
    @Nested
    @DisplayName("113. generateAndSaveFuturePlan")
    class GenerateAndSaveFuturePlanTests {

        @Test
        @DisplayName("TrÃƒÂ¡Ã‚ÂºÃ‚Â£ 404 khi sourceVersionId khÃƒÆ’Ã‚Â´ng tÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“n tÃƒÂ¡Ã‚ÂºÃ‚Â¡i")
        void generate_sourceVersionNotFound_returns404() {
            when(investmentProfileVersionRepository.findById(999)).thenReturn(Optional.empty());
            GenerateFuturePlanRequest req = new GenerateFuturePlanRequest();
            req.setSourceVersionId(999);

            ResponseEntity<ApiResponse> response = futurePlanService.generateAndSaveFuturePlan(req);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("TrÃƒÂ¡Ã‚ÂºÃ‚Â£ 400 khi khÃƒÆ’Ã‚Â´ng cÃƒÆ’Ã‚Â³ bÃƒÂ¡Ã‚ÂºÃ‚Â¥t Ãƒâ€žÃ¢â‚¬ËœÃƒÂ¡Ã‚Â»Ã¢â€žÂ¢ng sÃƒÂ¡Ã‚ÂºÃ‚Â£n nÃƒÆ’Ã‚Â o trong danh sÃƒÆ’Ã‚Â¡ch feedback")
        void generate_noSelectedProperties_returnsBadRequest() {
            when(investmentProfileVersionRepository.findById(6)).thenReturn(Optional.of(sourceVersion));
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            GenerateFuturePlanRequest req = new GenerateFuturePlanRequest();
            req.setSourceVersionId(6);
            req.setSelectedProperties(Collections.emptyList());

            ResponseEntity<ApiResponse> response = futurePlanService.generateAndSaveFuturePlan(req);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("TÃƒÂ¡Ã‚ÂºÃ‚Â¡o thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng, tÃƒÂ¡Ã‚Â»Ã‚Â± chuyÃƒÂ¡Ã‚Â»Ã†â€™n sang fallback rule-based khi AI lÃƒÂ¡Ã‚Â»Ã¢â‚¬â€i")
        void generate_success_withAiFallback() {
            when(investmentProfileVersionRepository.findById(6)).thenReturn(Optional.of(sourceVersion));
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            org.mockito.Mockito.lenient().when(futureInvestmentPlanRepository.countByInvestmentProfile_InvestmentProfileId(any()))
                    .thenReturn(0L);
            when(futureInvestmentPlanRepository.save(any(FutureInvestmentPlan.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            GenerateFuturePlanRequest.SelectedPropertyItem item = GenerateFuturePlanRequest.SelectedPropertyItem.builder()
                    .listingId(11)
                    .propertySource("SYSTEM")
                    .usagePurpose("CHO_THUE")
                    .monthlyRevenue(45_000_000L)
                    .monthlyOperatingCost(8_000_000L)
                    .actualPurchasePrice(13_500_000_000L)
                    .holdingMonths(6)
                    .build();

            GenerateFuturePlanRequest req = new GenerateFuturePlanRequest();
            req.setSourceVersionId(6);
            req.setSelectedProperties(List.of(item));

            ResponseEntity<ApiResponse> response = futurePlanService.generateAndSaveFuturePlan(req);

            // AI lÃƒÂ¡Ã‚Â»Ã¢â‚¬â€i (models=null) -> fallback rule-based -> vÃƒÂ¡Ã‚ÂºÃ‚Â«n tÃƒÂ¡Ã‚ÂºÃ‚Â¡o thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng.
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 114. View Future Investment Plan Details ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
    @Nested
    @DisplayName("114. getFuturePlanDetail")
    class GetFuturePlanDetailTests {
        @Test
        @DisplayName("TrÃƒÂ¡Ã‚ÂºÃ‚Â£ 404 khi kÃƒÂ¡Ã‚ÂºÃ‚Â¿ hoÃƒÂ¡Ã‚ÂºÃ‚Â¡ch tÃƒâ€ Ã‚Â°Ãƒâ€ Ã‚Â¡ng lai khÃƒÆ’Ã‚Â´ng tÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“n tÃƒÂ¡Ã‚ÂºÃ‚Â¡i")
        void getDetail_notFound_returns404() {
            when(futureInvestmentPlanRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = futurePlanService.getFuturePlanDetail(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Ãƒâ€žÃ‚ÂÃƒÂ¡Ã‚Â»Ã‚Âc lÃƒÂ¡Ã‚ÂºÃ‚Â¡i thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng tÃƒÂ¡Ã‚Â»Ã‚Â« snapshot Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â£ lÃƒâ€ Ã‚Â°u, khÃƒÆ’Ã‚Â´ng tÃƒÆ’Ã‚Â­nh lÃƒÂ¡Ã‚ÂºÃ‚Â¡i/khÃƒÆ’Ã‚Â´ng gÃƒÂ¡Ã‚Â»Ã‚Âi AI")
        void getDetail_success_readsFromSnapshot() {
            FutureInvestmentPlan plan = FutureInvestmentPlan.builder()
                    .futureInvestmentPlanId(1)
                    .name("Ket qua du doan 1")
                    .sourceVersion(sourceVersion)
                    .analysisSnapshot(Collections.emptyMap())
                    .build();
            when(futureInvestmentPlanRepository.findById(1)).thenReturn(Optional.of(plan));

            ResponseEntity<ApiResponse> response = futurePlanService.getFuturePlanDetail(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ 115. Get Derived Future Plans List ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬ÃƒÂ¢Ã¢â‚¬ÂÃ¢â€šÂ¬
    @Nested
    @DisplayName("115. getFutureVersionsBySourceVersionId")
    class GetFutureVersionsBySourceVersionIdTests {
        @Test
        @DisplayName("TrÃƒÂ¡Ã‚ÂºÃ‚Â£ 404 khi phiÃƒÆ’Ã‚Âªn bÃƒÂ¡Ã‚ÂºÃ‚Â£n gÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœc khÃƒÆ’Ã‚Â´ng tÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“n tÃƒÂ¡Ã‚ÂºÃ‚Â¡i")
        void getVersions_sourceNotFound_returns404() {
            when(investmentProfileVersionRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = futurePlanService.getFutureVersionsBySourceVersionId(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("TrÃƒÂ¡Ã‚ÂºÃ‚Â£ vÃƒÂ¡Ã‚Â»Ã‚Â danh sÃƒÆ’Ã‚Â¡ch rÃƒÂ¡Ã‚Â»Ã¢â‚¬â€ng khi phiÃƒÆ’Ã‚Âªn bÃƒÂ¡Ã‚ÂºÃ‚Â£n gÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœc chÃƒâ€ Ã‚Â°a cÃƒÆ’Ã‚Â³ kÃƒÂ¡Ã‚ÂºÃ‚Â¿ hoÃƒÂ¡Ã‚ÂºÃ‚Â¡ch tÃƒâ€ Ã‚Â°Ãƒâ€ Ã‚Â¡ng lai nÃƒÆ’Ã‚Â o")
        void getVersions_noneYet_returnsEmptyList() {
            when(investmentProfileVersionRepository.findById(6)).thenReturn(Optional.of(sourceVersion));
            when(futureInvestmentPlanRepository.findBySourceVersion_ProfileVersionIdOrderByCreatedAtDesc(6))
                    .thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse> response = futurePlanService.getFutureVersionsBySourceVersionId(6);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("TrÃƒÂ¡Ã‚ÂºÃ‚Â£ vÃƒÂ¡Ã‚Â»Ã‚Â danh sÃƒÆ’Ã‚Â¡ch kÃƒÂ¡Ã‚ÂºÃ‚Â¿ hoÃƒÂ¡Ã‚ÂºÃ‚Â¡ch tÃƒâ€ Ã‚Â°Ãƒâ€ Ã‚Â¡ng lai Ãƒâ€žÃ¢â‚¬ËœÃƒÆ’Ã‚Â£ tÃƒÂ¡Ã‚ÂºÃ‚Â¡o tÃƒÂ¡Ã‚Â»Ã‚Â« phiÃƒÆ’Ã‚Âªn bÃƒÂ¡Ã‚ÂºÃ‚Â£n gÃƒÂ¡Ã‚Â»Ã¢â‚¬Ëœc")
        void getVersions_hasResults_returnsList() {
            FutureInvestmentPlan plan = FutureInvestmentPlan.builder()
                    .futureInvestmentPlanId(1).name("Ket qua du doan 1")
                    .analysisSnapshot(Collections.emptyMap()).isActive(true).build();
            when(investmentProfileVersionRepository.findById(6)).thenReturn(Optional.of(sourceVersion));
            when(futureInvestmentPlanRepository.findBySourceVersion_ProfileVersionIdOrderByCreatedAtDesc(6))
                    .thenReturn(List.of(plan));

            ResponseEntity<ApiResponse> response = futurePlanService.getFutureVersionsBySourceVersionId(6);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }
}
