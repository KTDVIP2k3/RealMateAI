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
@DisplayName("InvestmentFuturePlanServiceImplement — Future Investment Plan")
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

    // ── 113. Create Future Investment Plan ────────────────────────────────
    @Nested
    @DisplayName("113. generateAndSaveFuturePlan")
    class GenerateAndSaveFuturePlanTests {

        @Test
        @DisplayName("Trả 404 khi sourceVersionId không tồn tại")
        void generate_sourceVersionNotFound_returns404() {
            when(investmentProfileVersionRepository.findById(999)).thenReturn(Optional.empty());
            GenerateFuturePlanRequest req = new GenerateFuturePlanRequest();
            req.setSourceVersionId(999);

            ResponseEntity<ApiResponse> response = futurePlanService.generateAndSaveFuturePlan(req);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả 400 khi không có bất động sản nào trong danh sách feedback")
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
        @DisplayName("Tạo thành công, tự chuyển sang fallback rule-based khi AI lỗi")
        void generate_success_withAiFallback() {
            when(investmentProfileVersionRepository.findById(6)).thenReturn(Optional.of(sourceVersion));
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);
            when(futureInvestmentPlanRepository.countByInvestmentProfile_InvestmentProfileId(any()))
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

            // AI lỗi (models=null) -> fallback rule-based -> vẫn tạo thành công.
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    // ── 114. View Future Investment Plan Details ──────────────────────────
    @Nested
    @DisplayName("114. getFuturePlanDetail")
    class GetFuturePlanDetailTests {
        @Test
        @DisplayName("Trả 404 khi kế hoạch tương lai không tồn tại")
        void getDetail_notFound_returns404() {
            when(futureInvestmentPlanRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = futurePlanService.getFuturePlanDetail(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Đọc lại thành công từ snapshot đã lưu, không tính lại/không gọi AI")
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

    // ── 115. Get Derived Future Plans List ────────────────────────────────
    @Nested
    @DisplayName("115. getFutureVersionsBySourceVersionId")
    class GetFutureVersionsBySourceVersionIdTests {
        @Test
        @DisplayName("Trả 404 khi phiên bản gốc không tồn tại")
        void getVersions_sourceNotFound_returns404() {
            when(investmentProfileVersionRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = futurePlanService.getFutureVersionsBySourceVersionId(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả về danh sách rỗng khi phiên bản gốc chưa có kế hoạch tương lai nào")
        void getVersions_noneYet_returnsEmptyList() {
            when(investmentProfileVersionRepository.findById(6)).thenReturn(Optional.of(sourceVersion));
            when(futureInvestmentPlanRepository.findBySourceVersion_ProfileVersionIdOrderByCreatedAtDesc(6))
                    .thenReturn(Collections.emptyList());

            ResponseEntity<ApiResponse> response = futurePlanService.getFutureVersionsBySourceVersionId(6);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Trả về danh sách kế hoạch tương lai đã tạo từ phiên bản gốc")
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
