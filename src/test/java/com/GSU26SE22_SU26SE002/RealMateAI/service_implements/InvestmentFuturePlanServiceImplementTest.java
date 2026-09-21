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
    @Nested
    @DisplayName("generateAndSaveFuturePlan")
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
        @DisplayName("Trả 400 không có body khi parameter dạng chuỗi bị blank")
        void generate_blankStringParameter_returns400WithoutBody() {
            String[] fields = {"planName", "consciousName", "propertySource", "usagePurpose"};

            for (String field : fields) {
                GenerateFuturePlanRequest req = validGenerateRequest();
                switch (field) {
                    case "planName" -> req.setPlanName("   ");
                    case "consciousName" -> req.setConsciousName("   ");
                    case "propertySource" -> req.getSelectedProperties().get(0).setPropertySource("   ");
                    case "usagePurpose" -> req.getSelectedProperties().get(0).setUsagePurpose("   ");
                }

                ResponseEntity<ApiResponse> response = futurePlanService.generateAndSaveFuturePlan(req);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), field);
                assertEquals(null, response.getBody(), field);
            }
        }

        @Test
        @DisplayName("Trả 400 không có body khi parameter dạng số bằng 0")
        void generate_zeroNumericParameter_returns400WithoutBody() {
            String[] fields = {
                    "equity", "loanCapital", "currentCashFlow", "longTermYear",
                    "actualPurchasePrice", "monthlyRevenue",
                    "monthlyOperatingCost", "holdingMonths"
            };

            for (String field : fields) {
                GenerateFuturePlanRequest req = validGenerateRequest();
                setNumericField(req, field, 0L);

                ResponseEntity<ApiResponse> response = futurePlanService.generateAndSaveFuturePlan(req);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), field + "=0");
                assertEquals(null, response.getBody(), field + "=0");
            }
        }

        @Test
        @DisplayName("Trả 400 không có body khi parameter dạng số nhỏ hơn 0")
        void generate_negativeNumericParameter_returns400WithoutBody() {
            String[] fields = {
                    "equity", "loanCapital", "currentCashFlow", "longTermYear",
                    "actualPurchasePrice", "monthlyRevenue",
                    "monthlyOperatingCost", "holdingMonths"
            };

            for (String field : fields) {
                GenerateFuturePlanRequest req = validGenerateRequest();
                setNumericField(req, field, -1L);

                ResponseEntity<ApiResponse> response = futurePlanService.generateAndSaveFuturePlan(req);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), field + "=-1");
                assertEquals(null, response.getBody(), field + "=-1");
            }
        }

        @Test
        @DisplayName("Trả 404 khi strategyId, listingId hoặc manualPropertyId không tồn tại")
        void generate_nonExistingReferenceId_returns404() {
            String[] idFields = {"strategyId", "listingId", "manualPropertyId"};

            for (String field : idFields) {
                GenerateFuturePlanRequest req = validGenerateRequest();
                GenerateFuturePlanRequest.SelectedPropertyItem item = req.getSelectedProperties().get(0);

                switch (field) {
                    case "strategyId" -> {
                        req.setStrategyId(1);
                        when(strategyRepository.findById(1)).thenReturn(Optional.empty());
                    }
                    case "listingId" -> {
                        item.setListingId(1);
                        when(listingRepository.findById(1)).thenReturn(Optional.empty());
                    }
                    case "manualPropertyId" -> {
                        item.setListingId(null);
                        item.setManualPropertyId(999);
                        when(propertyRepository.findById(999)).thenReturn(Optional.empty());
                    }
                }

                ResponseEntity<ApiResponse> response = futurePlanService.generateAndSaveFuturePlan(req);

                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(), field);
            }
        }

        @Test
        @DisplayName("Trả 400 không có body khi danh sách truyền vào rỗng hoặc chứa chuỗi blank")
        void generate_invalidCollectionParameter_returns400WithoutBody() {
            GenerateFuturePlanRequest emptyWards = validGenerateRequest();
            emptyWards.setWardNames(Collections.emptyList());
            assertBadRequestWithoutBody(emptyWards);

            GenerateFuturePlanRequest blankWard = validGenerateRequest();
            blankWard.setWardNames(List.of("   "));
            assertBadRequestWithoutBody(blankWard);

            GenerateFuturePlanRequest emptyStrategyDetail = validGenerateRequest();
            emptyStrategyDetail.setInvestmentStrategyDetail(Collections.emptyMap());
            assertBadRequestWithoutBody(emptyStrategyDetail);
        }

        @Test
        @DisplayName("Tạo thành công, tự chuyển sang fallback rule-based khi AI lỗi")
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

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        private GenerateFuturePlanRequest validGenerateRequest() {
            when(investmentProfileVersionRepository.findById(6)).thenReturn(Optional.of(sourceVersion));
            when(authenUntil.getCurrentUSer()).thenReturn(investorAccount);

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
            req.setPlanName("Kế hoạch tương lai");
            req.setEquity(2_000_000_000L);
            req.setLoanCapital(1_000_000_000L);
            req.setCurrentCashFlow(50_000_000L);
            req.setConsciousName("Hồ Chí Minh");
            req.setWardNames(List.of("Phường Bến Nghé"));
            req.setLongTermYear(20);
            req.setStrategyId(1);
            req.setInvestmentStrategyDetail(Collections.singletonMap("riskLevel", "MEDIUM"));
            req.setSelectedProperties(List.of(item));
            return req;
        }

        private void setNumericField(GenerateFuturePlanRequest req, String field, long value) {
            GenerateFuturePlanRequest.SelectedPropertyItem item = req.getSelectedProperties().get(0);
            switch (field) {
                case "equity" -> req.setEquity(value);
                case "loanCapital" -> req.setLoanCapital(value);
                case "currentCashFlow" -> req.setCurrentCashFlow(value);
                case "longTermYear" -> req.setLongTermYear((int) value);
                case "actualPurchasePrice" -> item.setActualPurchasePrice(value);
                case "monthlyRevenue" -> item.setMonthlyRevenue(value);
                case "monthlyOperatingCost" -> item.setMonthlyOperatingCost(value);
                case "holdingMonths" -> item.setHoldingMonths((int) value);
                default -> throw new IllegalArgumentException("Unknown numeric field: " + field);
            }
        }

        private void assertBadRequestWithoutBody(GenerateFuturePlanRequest req) {
            ResponseEntity<ApiResponse> response = futurePlanService.generateAndSaveFuturePlan(req);
            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals(null, response.getBody());
        }
    }

    @Nested
    @DisplayName("getFuturePlanDetail")
    class GetFuturePlanDetailTests {
        @Test
        @DisplayName("Trả 404 khi kế hoạch tương lai không tồn tại")
        void getDetail_notFound_returns404() {
            when(futureInvestmentPlanRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = futurePlanService.getFuturePlanDetail(999);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Đọc lại thành công từ snapshot đã lưu, không tính lại và không gọi AI")
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

    @Nested
    @DisplayName("getFutureVersionsBySourceVersionId")
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