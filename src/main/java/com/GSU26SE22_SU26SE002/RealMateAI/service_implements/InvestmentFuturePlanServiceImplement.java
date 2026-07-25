package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.GenerateFuturePlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestmentFuturePlanServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Future Plan = một InvestmentProfileVersion mới (version = "FUTURE_PLAN", baseVersion = source).
 * Logic lưu trữ TÁI SỬ DỤNG đúng các repository và đúng cấu trúc bảng đã có
 * (InvestmentScenario, ExecutionPlan, InvestmentPortfolio, PortfolioAllocation,
 * PortfolioAllocationProperty) — giống hệt cách saveUpdatePlanToDatabase() trong
 * InvestmentPlanServiceImplement đã làm cho luồng AI Stage1-3 thông thường.
 *
 * KHÔNG có bảng investment_future_plan hay future_plan_property_feedback riêng:
 *  - "future plan" chỉ khác ở NGUỒN dữ liệu input (feedback thực tế thay vì AI đề xuất),
 *    không khác về MÔ HÌNH dữ liệu đầu ra. Đầu ra vẫn là 1 version với đủ scenarios/
 *    executionPlan/portfolios/properties như mọi version khác — nên tái dùng đúng bảng đó.
 */

@Slf4j
@Service
public class InvestmentFuturePlanServiceImplement implements InvestmentFuturePlanServiceInterface {

    @Autowired
    private Client geminiClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenUntil authenUntil;

    @Autowired
    private InvestmentProfileVersionRepository investmentProfileVersionRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private InvestmentPortfolioRepository investmentPortfolioRepository;

    @Autowired
    private PortfolioAllocationRepository portfolioAllocationRepository;

    @Autowired
    private PortfolioAllocationPropertyRepository portfolioAllocationPropertyRepository;

    @Autowired
    private InvestmentScenarioRepository investmentScenarioRepository;

    @Autowired
    private ExecutionPlanRepository executionPlanRepository;

    @Autowired
    private InvestmentCriteriaRepository investmentCriteriaRepository;

    // =====================================================================
    // GENERATE + SAVE — 1 transaction duy nhất, không có bước "preview" rời
    // =====================================================================

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> generateAndSaveFuturePlan(GenerateFuturePlanRequest request) {
        try {
            InvestmentProfileVersion sourceVersion = investmentProfileVersionRepository
                    .findById(request.getSourceVersionId()).orElse(null);
            if (sourceVersion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Version_Not_Found", "Không tìm thấy phiên bản kế hoạch gốc với ID: " + request.getSourceVersionId()));
            }

            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null || currentAccount.getInvestor() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Không tìm thấy thông tin nhà đầu tư."));
            }

            if (request.getSelectedProperties() == null || request.getSelectedProperties().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("No_Properties", "Vui lòng chọn ít nhất 1 sản phẩm bất động sản."));
            }

            // 1. Tính lợi nhuận từng property (thuần Java, không cần AI vì đây là phép toán xác định)
            List<PropertyProfitResultDTO> profitResults = calculatePropertyProfits(request.getSelectedProperties());
            PortfolioAggregation aggregation = aggregate(profitResults);

            // 2. Gọi AI sinh scenarios + executionPlan dựa trên số liệu THỰC TẾ vừa tính
            InvestmentPlanDTO aiOutput = callGeminiForFutureScenarios(sourceVersion, profitResults, aggregation);

            // 3. Gọi AI sinh nhận xét so sánh ngắn gọn
            InvestmentFuturePlanDTO.ComparisonSummaryDTO comparison = buildComparisonSummary(sourceVersion, aggregation);

            // 4. Deactivate version cũ (logic version-control giống hệt updateExistingInvestmentPlan)
            InvestmentProfile profile = sourceVersion.getInvestmentProfile();
            LocalDateTime now = LocalDateTime.now();
            if (profile != null && profile.getProfileVersions() != null) {
                for (InvestmentProfileVersion v : profile.getProfileVersions()) {
                    if (Boolean.TRUE.equals(v.getIsActive())) {
                        v.setIsActive(false);
                        v.setUpdatedAt(now);
                        investmentProfileVersionRepository.save(v);
                    }
                }
            }

            // 5. Tạo InvestmentProfileVersion mới — clone tham số gốc + đánh dấu FUTURE_PLAN
            int nextNum = (profile != null && profile.getProfileVersions() != null)
                    ? profile.getProfileVersions().size() + 1 : 2;
            String autoVersionName = request.getPlanName() != null ? request.getPlanName()
                    : sourceVersion.getProfileVersionName() + " - Future V" + nextNum;

            Map<String, Object> profitSummaryMap = buildProfitSummaryMap(aggregation, comparison, aiOutput.getScore());

            InvestmentProfileVersion newVersion = InvestmentProfileVersion.builder()
                    .investmentProfile(profile)
                    .profileVersionName(autoVersionName)
                    .strategy(sourceVersion.getStrategy())
                    .equity(sourceVersion.getEquity())
                    .loanCapital(sourceVersion.getLoanCapital())
                    .reserveFund(sourceVersion.getReserveFund())
                    .conscious(sourceVersion.getConscious())
                    .ward(sourceVersion.getWard())
                    .expectedRoi(sourceVersion.getExpectedRoi())
                    .riskToleranceLevel(sourceVersion.getRiskToleranceLevel())
                    .durationYear(sourceVersion.getDurationYear())
                    .startDate(sourceVersion.getStartDate())
                    .investmentStrategyDetail(sourceVersion.getInvestmentStrategyDetail())
                    .legalStatus(sourceVersion.getLegalStatus())
                    .version("FUTURE_PLAN")
                    .baseVersion(sourceVersion)
                    .profitSummary(profitSummaryMap)
                    .isActive(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .investmentCriterias(new ArrayList<>())
                    .investmentScenarios(new ArrayList<>())
                    .investmentPortfolios(new ArrayList<>())
                    .executionPlans(new ArrayList<>())
                    .build();

            InvestmentProfileVersion savedVersion = investmentProfileVersionRepository.save(newVersion);

            // 6. Copy criteria từ source (giữ nguyên tiêu chí lọc gốc)
            if (sourceVersion.getInvestmentCriterias() != null) {
                for (InvestmentCriteria srcCrit : sourceVersion.getInvestmentCriterias()) {
                    investmentCriteriaRepository.save(InvestmentCriteria.builder()
                            .investmentProfileVersion(savedVersion)
                            .propertyType(srcCrit.getPropertyType())
                            .propertyCondition(srcCrit.getPropertyCondition())
                            .build());
                }
            }

            // 7. Lưu scenarios từ AI output
            if (aiOutput.getScenarios() != null) {
                for (InvestmentScenarioDTO s : aiOutput.getScenarios()) {
                    investmentScenarioRepository.save(InvestmentScenario.builder()
                            .investmentProfileVersion(savedVersion)
                            .name(s.getEnumScenarioType())
                            .scenarioType(s.getEnumScenarioType())
                            .expectedReturnRate(s.getDecimprofitYield())
                            .description(s.getTextMarketNote())
                            .isActive(true)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                }
            }

            // 8. Lưu execution plan
            if (aiOutput.getExecutionPlan() != null) {
                String descJson = objectMapper.writeValueAsString(aiOutput.getExecutionPlan());
                executionPlanRepository.save(ExecutionPlan.builder()
                        .investmentProfileVersion(savedVersion)
                        .name("AI Future Execution Plan")
                        .description(descJson)
                        .status("ACTIVE")
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }

            // 9. Lưu portfolio + properties — ghi TRỰC TIẾP vào PortfolioAllocationProperty,
            //    không qua bảng trung gian nào. Group theo portfolioId investor gửi lên.
            //    KHÔNG còn build InvestmentPortfolioDTO/PortfolioAllocationPropertyDTO ở đây
            //    nữa — response tạo mới giờ chỉ trả newVersionId (xem bước 10), FE gọi GET
            //    /investment-plans/future/{newVersionId} để lấy lại đúng cấu trúc này (xem
            //    getFuturePlanDetail — nguồn DUY NHẤT lắp ráp investmentPortfolios).
            Map<Integer, List<GenerateFuturePlanRequest.SelectedPropertyItem>> byPortfolio = request.getSelectedProperties()
                    .stream()
                    .filter(item -> item.getPortfolioId() != null)
                    .collect(Collectors.groupingBy(GenerateFuturePlanRequest.SelectedPropertyItem::getPortfolioId));

            for (Map.Entry<Integer, List<GenerateFuturePlanRequest.SelectedPropertyItem>> entry : byPortfolio.entrySet()) {
                Portfolio portfolio = portfolioRepository.findById(entry.getKey()).orElse(null);
                if (portfolio == null) continue;

                List<GenerateFuturePlanRequest.SelectedPropertyItem> items = entry.getValue();
                long capitalForThisPortfolio = items.stream()
                        .mapToLong(i -> i.getActualPurchasePrice() != null ? i.getActualPurchasePrice() : 0L)
                        .sum();

                InvestmentPortfolio investmentPortfolio = investmentPortfolioRepository.save(InvestmentPortfolio.builder()
                        .investmentProfileVersion(savedVersion)
                        .portfolio(portfolio)
                        .percentage(0) // future plan không tính lại % phân bổ AI, giữ 0 vì capital đã thực
                        .capital(capitalForThisPortfolio)
                        .isActive(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

                PortfolioAllocation allocation = portfolioAllocationRepository.save(PortfolioAllocation.builder()
                        .portfolio(portfolio)
                        .investmentPortfolio(investmentPortfolio)
                        .isActive(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());

                for (GenerateFuturePlanRequest.SelectedPropertyItem item : items) {
                    Property property = resolveProperty(item);
                    if (property == null) continue;

                    portfolioAllocationPropertyRepository.save(
                            PortfolioAllocationProperty.builder()
                                    .portfolioAllocation(allocation)
                                    .property(property)
                                    .weight(1.0)
                                    .propertySource(item.getPropertySource() != null ? item.getPropertySource() : "SYSTEM")
                                    .usagePurpose(item.getUsagePurpose())
                                    .monthlyRevenue(item.getMonthlyRevenue())
                                    .monthlyOperatingCost(item.getMonthlyOperatingCost())
                                    .actualPurchasePrice(item.getActualPurchasePrice())
                                    .evaluatedMarketPrice(item.getEvaluatedMarketPrice())
                                    .holdingMonths(resolveHoldingMonths(item.getHoldingMonths()))
                                    .isSelected(true)
                                    .isActive(true)
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .build());
                }
            }

            // 10. Response GỌN — chỉ xác nhận đã tạo + newVersionId để FE gọi
            //     GET /investment-plans/future/{newVersionId} lấy output đầy đủ.
            FuturePlanCreatedResponse responseDTO = FuturePlanCreatedResponse.builder()
                    .newVersionId(savedVersion.getProfileVersionId())
                    .newVersionName(savedVersion.getProfileVersionName())
                    .sourceVersionId(sourceVersion.getProfileVersionId())
                    .sourceVersionName(sourceVersion.getProfileVersionName())
                    .createdAt(savedVersion.getCreatedAt())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(responseDTO, "Tạo và lưu phiên bản kế hoạch tương lai thành công"));

        } catch (Exception e) {
            log.error("Error in generateAndSaveFuturePlan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }

    // =====================================================================
    // GET DETAIL — đọc lại y hệt cấu trúc đã lưu, không cần parse JSON blob riêng
    // =====================================================================

    @Transactional
    @Override
    public ResponseEntity<ApiResponse> getFuturePlanDetail(Integer versionId) {
        try {
            InvestmentProfileVersion version = investmentProfileVersionRepository.findById(versionId).orElse(null);
            if (version == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Version_Not_Found", "Không tìm thấy phiên bản: " + versionId));
            }
            if (!"FUTURE_PLAN".equals(version.getVersion())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Not_Future_Plan", "Phiên bản này không phải kế hoạch tương lai."));
            }

            List<InvestmentScenarioDTO> scenarioDTOs = version.getInvestmentScenarios() == null ? List.of()
                    : version.getInvestmentScenarios().stream().map(s -> InvestmentScenarioDTO.builder()
                    .enumScenarioType(s.getScenarioType())
                    .decimprofitYield(s.getExpectedReturnRate())
                    .textMarketNote(s.getDescription())
                    .build()).collect(Collectors.toList());

            ExecutionPlanDTO executionPlanDTO = null;
            if (version.getExecutionPlans() != null && !version.getExecutionPlans().isEmpty()) {
                String desc = version.getExecutionPlans().get(0).getDescription();
                if (desc != null) {
                    executionPlanDTO = objectMapper.readValue(desc, ExecutionPlanDTO.class);
                }
            }

            // Đọc trực tiếp profitSummary đã lưu sẵn ở cấp version — không cần tính lại
            Map<String, Object> profitSummary = version.getProfitSummary() != null ? version.getProfitSummary() : Map.of();

            // ── Tái dựng investmentPortfolios + propertyProfitResults từ
            // InvestmentPortfolio -> PortfolioAllocation -> PortfolioAllocationProperty
            // đã lưu lúc tạo (KHÔNG còn nhận trực tiếp từ response tạo mới — xem
            // generateAndSaveFuturePlan đã bỏ build 2 phần này khỏi response). Lợi
            // nhuận từng property TÍNH LẠI bằng đúng computeProfitResult() (dùng
            // chung công thức với lúc tạo) từ các field đã lưu, bao gồm cả
            // holdingMonths (mới thêm cột để lưu đúng số tháng investor đã nhập).
            List<InvestmentPortfolioDTO> investmentPortfolios = new ArrayList<>();
            List<PropertyProfitResultDTO> propertyProfitResults = new ArrayList<>();

            List<InvestmentPortfolio> portfolios = version.getInvestmentPortfolios() != null
                    ? version.getInvestmentPortfolios() : List.of();
            for (InvestmentPortfolio ip : portfolios) {
                List<PortfolioAllocation> allocations = portfolioAllocationRepository
                        .findByInvestmentPortfolio_InvestmentPortfolioId(ip.getInvestmentPortfolioId());

                List<PortfolioAllocationPropertyDTO> propertyDTOs = new ArrayList<>();
                for (PortfolioAllocation alloc : allocations) {
                    List<PortfolioAllocationProperty> paps = alloc.getPortfolioAllocationProperties() != null
                            ? alloc.getPortfolioAllocationProperties() : List.of();
                    for (PortfolioAllocationProperty pap : paps) {
                        Property property = pap.getProperty();
                        String propertyName = property != null && property.getTitle() != null
                                ? property.getTitle() : "Bất động sản";

                        propertyDTOs.add(PortfolioAllocationPropertyDTO.builder()
                                .portfolioAllocationPropertyId(pap.getPortfolioAllocationPropertyId())
                                .propertyProjectName(propertyName)
                                .area(property != null && property.getArea() != null ? property.getArea().intValue() : 0)
                                .valuePrice(pap.getActualPurchasePrice() != null ? pap.getActualPurchasePrice().doubleValue() : 0.0)
                                .description(pap.getUsagePurpose())
                                .build());

                        if (pap.getActualPurchasePrice() != null && pap.getActualPurchasePrice() > 0) {
                            long initialPrice = pap.getActualPurchasePrice();
                            long evaluatedPrice = pap.getEvaluatedMarketPrice() != null ? pap.getEvaluatedMarketPrice() : initialPrice;
                            long monthlyRevenue = pap.getMonthlyRevenue() != null ? pap.getMonthlyRevenue() : 0L;
                            long monthlyOpCost = pap.getMonthlyOperatingCost() != null ? pap.getMonthlyOperatingCost() : 0L;
                            int holdingMonths = resolveHoldingMonths(pap.getHoldingMonths());

                            // LƯU Ý: PortfolioAllocationProperty chỉ lưu FK tới Property, KHÔNG
                            // lưu lại listingId gốc (property SYSTEM có thể đã bị gỡ/đổi listing
                            // khác sau này, và property MANUAL vốn dĩ không có listingId). Dùng
                            // propertyId thay thế cho field "listingId" của PropertyProfitResultDTO
                            // — đủ để FE định danh property, KHÔNG hoàn toàn tương đương listingId
                            // gốc lúc investor chọn (khác nhẹ so với lúc tạo — chấp nhận được vì
                            // property KHÔNG đổi, chỉ có thể có nhiều/không có listing bao quanh nó).
                            propertyProfitResults.add(computeProfitResult(
                                    property != null ? property.getPropertyId() : null,
                                    propertyName, pap.getUsagePurpose(),
                                    initialPrice, evaluatedPrice, monthlyRevenue, monthlyOpCost, holdingMonths));
                        }
                    }
                }

                investmentPortfolios.add(InvestmentPortfolioDTO.builder()
                        .portfolioId(ip.getPortfolio() != null ? ip.getPortfolio().getPortfolioId() : null)
                        .portfolioName(ip.getPortfolio() != null ? ip.getPortfolio().getName() : null)
                        .percentage(ip.getPercentage())
                        .capital(ip.getCapital())
                        .allocations(List.of(PortfolioAllocationDTO.builder()
                                .propertyTypeName(ip.getPortfolio() != null ? ip.getPortfolio().getName() : null)
                                .properties(propertyDTOs)
                                .build()))
                        .build());
            }

            InvestmentFuturePlanDTO.ComparisonSummaryDTO comparisonDTO = InvestmentFuturePlanDTO.ComparisonSummaryDTO.builder()
                    .originalExpectedYield(profitSummary.get("originalExpectedYield") != null
                            ? ((Number) profitSummary.get("originalExpectedYield")).doubleValue() : null)
                    .actualCalculatedYield(((Number) profitSummary.getOrDefault("totalProfitPercentage", 0.0)).doubleValue())
                    .yieldDelta(profitSummary.get("yieldDelta") != null
                            ? ((Number) profitSummary.get("yieldDelta")).doubleValue() : null)
                    .aiComparisonNote((String) profitSummary.get("aiComparisonNote"))
                    .aiActionRecommendation((String) profitSummary.get("aiActionRecommendation"))
                    .build();

            InvestmentFuturePlanDTO dto = InvestmentFuturePlanDTO.builder()
                    .newVersionId(version.getProfileVersionId())
                    .newVersionName(version.getProfileVersionName())
                    .sourceVersionId(version.getBaseVersion() != null ? version.getBaseVersion().getProfileVersionId() : null)
                    .sourceVersionName(version.getBaseVersion() != null ? version.getBaseVersion().getProfileVersionName() : null)
                    .scenarios(scenarioDTOs)
                    .executionPlan(executionPlanDTO)
                    .investmentPortfolios(investmentPortfolios)
                    .propertyProfitResults(propertyProfitResults)
                    .totalInvestedCapital(((Number) profitSummary.getOrDefault("totalInvestedCapital", 0L)).longValue())
                    .totalMonthlyNetCashflow(((Number) profitSummary.getOrDefault("totalMonthlyNetCashflow", 0L)).longValue())
                    .totalRentalIncomeAccumulated(((Number) profitSummary.getOrDefault("totalRentalIncome", 0L)).longValue())
                    .totalPortfolioProfitAmount(((Number) profitSummary.getOrDefault("totalProfitAmount", 0L)).longValue())
                    .totalPortfolioProfitPercentage(((Number) profitSummary.getOrDefault("totalProfitPercentage", 0.0)).doubleValue())
                    .portfolioScore(profitSummary.get("portfolioScore") != null
                            ? ((Number) profitSummary.get("portfolioScore")).intValue() : null)
                    .comparisonWithSource(comparisonDTO)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(dto, "Lấy chi tiết kế hoạch tương lai thành công"));

        } catch (Exception e) {
            log.error("Error in getFuturePlanDetail", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // =====================================================================
    // PRIVATE HELPERS
    // =====================================================================

    private Property resolveProperty(GenerateFuturePlanRequest.SelectedPropertyItem item) {
        if ("MANUAL".equalsIgnoreCase(item.getPropertySource())) {
            if (item.getManualPropertyId() == null) return null;
            return propertyRepository.findById(item.getManualPropertyId()).orElse(null);
        }
        // SYSTEM: resolve qua listing
        if (item.getListingId() == null) return null;
        Listing listing = listingRepository.findById(item.getListingId()).orElse(null);
        return listing != null ? listing.getProperty() : null;
    }

    /** Mặc định 6 tháng khi investor không nhập hoặc nhập giá trị không hợp lệ (&lt;= 0). */
    private static int resolveHoldingMonths(Integer raw) {
        return raw != null && raw > 0 ? raw : 6;
    }

    /**
     * Công thức tính lợi nhuận DÙNG CHUNG — được gọi ở 2 nơi:
     *  (1) calculatePropertyProfits() lúc TẠO (input từ GenerateFuturePlanRequest),
     *  (2) getFuturePlanDetail() lúc ĐỌC LẠI (input từ PortfolioAllocationProperty đã lưu).
     * Tách ra để 2 nơi này KHÔNG BAO GIỜ lệch công thức với nhau.
     */
    private PropertyProfitResultDTO computeProfitResult(Integer listingId, String propertyName, String usagePurpose,
                                                        long initialPrice, long evaluatedPrice,
                                                        long monthlyRevenue, long monthlyOpCost, int holdingMonths) {
        long monthlyNetCashflow = monthlyRevenue - monthlyOpCost;
        long capitalGain = evaluatedPrice - initialPrice;
        long totalRentalIncome = monthlyNetCashflow * holdingMonths;
        long totalProfit = capitalGain + totalRentalIncome;
        double profitPct = initialPrice > 0 ? ((double) totalProfit / initialPrice) * 100.0 : 0.0;
        double annualizedYield = holdingMonths > 0 ? profitPct / (holdingMonths / 12.0) : 0.0;

        return PropertyProfitResultDTO.builder()
                .listingId(listingId)
                .propertyName(propertyName)
                .usagePurpose(usagePurpose)
                .initialPrice(initialPrice)
                .evaluatedMarketPrice(evaluatedPrice)
                .capitalGain(capitalGain)
                .monthlyNetCashflow(monthlyNetCashflow)
                .totalRentalIncome(totalRentalIncome)
                .holdingMonths(holdingMonths)
                .totalProfitAmount(totalProfit)
                .profitPercentage(Math.round(profitPct * 100.0) / 100.0)
                .annualizedYield(Math.round(annualizedYield * 100.0) / 100.0)
                .build();
    }

    private List<PropertyProfitResultDTO> calculatePropertyProfits(List<GenerateFuturePlanRequest.SelectedPropertyItem> items) {
        List<PropertyProfitResultDTO> results = new ArrayList<>();
        for (GenerateFuturePlanRequest.SelectedPropertyItem item : items) {
            if (item.getActualPurchasePrice() == null || item.getActualPurchasePrice() <= 0) continue;

            long initialPrice = item.getActualPurchasePrice();
            long evaluatedPrice = item.getEvaluatedMarketPrice() != null ? item.getEvaluatedMarketPrice() : initialPrice;
            long monthlyRevenue = item.getMonthlyRevenue() != null ? item.getMonthlyRevenue() : 0L;
            long monthlyOpCost = item.getMonthlyOperatingCost() != null ? item.getMonthlyOperatingCost() : 0L;
            int holdingMonths = resolveHoldingMonths(item.getHoldingMonths());

            String propertyName = "Bất động sản";
            Property property = resolveProperty(item);
            if (property != null && property.getTitle() != null) propertyName = property.getTitle();

            results.add(computeProfitResult(item.getListingId(), propertyName, item.getUsagePurpose(),
                    initialPrice, evaluatedPrice, monthlyRevenue, monthlyOpCost, holdingMonths));
        }
        return results;
    }

    private record PortfolioAggregation(
            long totalInvestedCapital, long totalMonthlyNetCashflow,
            long totalRentalIncome, long totalProfitAmount, double totalProfitPercentage) {}

    private PortfolioAggregation aggregate(List<PropertyProfitResultDTO> results) {
        long invested = results.stream().mapToLong(r -> r.getInitialPrice() != null ? r.getInitialPrice() : 0L).sum();
        long monthly = results.stream().mapToLong(r -> r.getMonthlyNetCashflow() != null ? r.getMonthlyNetCashflow() : 0L).sum();
        long rental = results.stream().mapToLong(r -> r.getTotalRentalIncome() != null ? r.getTotalRentalIncome() : 0L).sum();
        long profit = results.stream().mapToLong(r -> r.getTotalProfitAmount() != null ? r.getTotalProfitAmount() : 0L).sum();
        double pct = invested > 0 ? ((double) profit / invested) * 100.0 : 0.0;
        return new PortfolioAggregation(invested, monthly, rental, profit, Math.round(pct * 100.0) / 100.0);
    }

    private Map<String, Object> buildProfitSummaryMap(PortfolioAggregation agg, InvestmentFuturePlanDTO.ComparisonSummaryDTO comparison, Integer portfolioScore) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalInvestedCapital", agg.totalInvestedCapital());
        map.put("totalMonthlyNetCashflow", agg.totalMonthlyNetCashflow());
        map.put("totalRentalIncome", agg.totalRentalIncome());
        map.put("totalProfitAmount", agg.totalProfitAmount());
        map.put("totalProfitPercentage", agg.totalProfitPercentage());
        map.put("portfolioScore", portfolioScore);
        if (comparison != null) {
            map.put("originalExpectedYield", comparison.getOriginalExpectedYield());
            map.put("yieldDelta", comparison.getYieldDelta());
            map.put("aiComparisonNote", comparison.getAiComparisonNote());
            map.put("aiActionRecommendation", comparison.getAiActionRecommendation());
        }
        return map;
    }

    private InvestmentPlanDTO callGeminiForFutureScenarios(
            InvestmentProfileVersion sourceVersion,
            List<PropertyProfitResultDTO> profitResults,
            PortfolioAggregation aggregation) throws Exception {

        String profitSummaryJson = objectMapper.writeValueAsString(profitResults);

        String prompt = "Bạn là chuyên gia phân tích tài chính bất động sản.\n\n" +
                "Kế hoạch gốc: Vốn tự có " + sourceVersion.getEquity() + " VNĐ, Vốn vay " + sourceVersion.getLoanCapital() +
                " VNĐ, ROI kỳ vọng " + sourceVersion.getExpectedRoi() + "%, thời gian " + sourceVersion.getDurationYear() + " năm.\n\n" +
                "Nhà đầu tư đã THỰC TẾ chọn các bất động sản với kết quả tính toán thực:\n" + profitSummaryJson + "\n\n" +
                "Tổng hợp: Vốn đầu tư " + aggregation.totalInvestedCapital() + " VNĐ, dòng tiền thuần/tháng " +
                aggregation.totalMonthlyNetCashflow() + " VNĐ, lợi nhuận tổng " + aggregation.totalProfitAmount() +
                " VNĐ (" + aggregation.totalProfitPercentage() + "%).\n\n" +
                "YÊU CẦU: Phân tích 3 kịch bản thị trường tương lai dựa trên số liệu THỰC TẾ trên, lập kế hoạch hành động. " +
                "TOÀN BỘ tiếng Việt. enumScenarioType chỉ dùng: 'xu hướng tăng', 'trung bình', 'xu hướng giảm'.";

        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .responseSchema(Schema.builder()
                        .type("OBJECT")
                        .properties(Map.of(
                                "score", Schema.builder().type("INTEGER").build(),
                                "scenarios", Schema.builder().type("ARRAY").items(Schema.builder()
                                        .type("OBJECT").properties(Map.of(
                                                "pkInvestmentScenarioId", Schema.builder().type("INTEGER").build(),
                                                "enumScenarioType", Schema.builder().type("STRING").build(),
                                                "decimprofitYield", Schema.builder().type("NUMBER").build(),
                                                "decimmonthlyCashflow", Schema.builder().type("NUMBER").build(),
                                                "decimprobability", Schema.builder().type("NUMBER").build(),
                                                "textMarketNote", Schema.builder().type("STRING").build(),
                                                "durationMonths", Schema.builder().type("INTEGER").build(),
                                                "decimpriceGrowthMin", Schema.builder().type("NUMBER").build(),
                                                "decimpriceGrowthMax", Schema.builder().type("NUMBER").build()
                                        )).build()).build(),
                                "executionPlan", Schema.builder().type("OBJECT").properties(Map.ofEntries(
                                        Map.entry("pkExecutionPlanId", Schema.builder().type("INTEGER").build()),
                                        Map.entry("totalInvestmentCapital", Schema.builder().type("NUMBER").build()),
                                        Map.entry("decimloanPercentage", Schema.builder().type("NUMBER").build()),
                                        Map.entry("decimmonthlyPayment", Schema.builder().type("NUMBER").build()),
                                        Map.entry("decimprobability", Schema.builder().type("NUMBER").build()),
                                        Map.entry("decimnetCashflow", Schema.builder().type("NUMBER").build()),
                                        Map.entry("maxHoldingMonths", Schema.builder().type("INTEGER").build()),
                                        Map.entry("booleanIsLegalClear", Schema.builder().type("BOOLEAN").build()),
                                        Map.entry("booleanIsLeverageSafe", Schema.builder().type("BOOLEAN").build()),
                                        Map.entry("stringLiquidityDurationRange", Schema.builder().type("STRING").build()),
                                        Map.entry("booleanIsReserveFundEnough", Schema.builder().type("BOOLEAN").build()),
                                        Map.entry("textTakeProfitStrategy", Schema.builder().type("STRING").build()),
                                        Map.entry("textHoldingTimeLimit", Schema.builder().type("STRING").build()),
                                        Map.entry("textQuickSellAction", Schema.builder().type("STRING").build())
                                )).build()
                        ))
                        .required(List.of("score", "scenarios", "executionPlan"))
                        .build())
                .build();

        int retryCount = 0;
        GenerateContentResponse response = null;
        while (retryCount < 5) {
            try {
                response = geminiClient.models.generateContent("gemini-2.5-flash", prompt, config);
                break;
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("429") || msg.contains("503") || msg.contains("Quota exceeded") || msg.contains("Unavailable")) {
                    retryCount++;
                    try { Thread.sleep(8000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); throw new RuntimeException(ie); }
                } else throw e;
            }
        }
        if (response == null) throw new RuntimeException("Gemini API quá tải. Vui lòng thử lại.");
        return objectMapper.readValue(response.text().trim(), InvestmentPlanDTO.class);
    }

    // =====================================================================
    // GET /investment-plans/future/by-source/{sourceVersionId} — danh sách
    // TÓM TẮT các future-version phái sinh từ 1 version gốc (bước giữa giữa
    // "get all version" và "get future-version detail" — xem javadoc interface)
    // =====================================================================

    @Override
    public ResponseEntity<ApiResponse> getFutureVersionsBySourceVersionId(Integer sourceVersionId) {
        try {
            InvestmentProfileVersion sourceVersion = investmentProfileVersionRepository
                    .findById(sourceVersionId).orElse(null);
            if (sourceVersion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Version_Not_Found", "Không tìm thấy phiên bản gốc với ID: " + sourceVersionId));
            }

            List<InvestmentProfileVersion> futureVersions = investmentProfileVersionRepository
                    .findByBaseVersion_ProfileVersionIdOrderByCreatedAtDesc(sourceVersionId);

            List<FutureVersionSummaryDTO> result = futureVersions.stream()
                    .map(v -> {
                        Map<String, Object> summary = v.getProfitSummary() != null ? v.getProfitSummary() : Map.of();
                        return FutureVersionSummaryDTO.builder()
                                .futureVersionId(v.getProfileVersionId())
                                .futureVersionName(v.getProfileVersionName())
                                .sourceVersionId(sourceVersionId)
                                .actualCalculatedYield(toDoubleOrNull(summary.get("totalProfitPercentage")))
                                .yieldDelta(toDoubleOrNull(summary.get("yieldDelta")))
                                .portfolioScore(toIntegerOrNull(summary.get("portfolioScore")))
                                .isActive(v.getIsActive())
                                .createdAt(v.getCreatedAt())
                                .build();
                    })
                    .collect(Collectors.toList());

            String msg = result.isEmpty()
                    ? "Phiên bản này chưa có kế hoạch tương lai nào được tạo"
                    : "Danh sách " + result.size() + " kế hoạch tương lai phái sinh từ phiên bản này";

            return ResponseEntity.ok(ApiResponse.success(result, msg));
        } catch (Exception e) {
            log.error("[InvestmentFuturePlanService] getFutureVersionsBySourceVersionId lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private static Double toDoubleOrNull(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : null;
    }

    private static Integer toIntegerOrNull(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : null;
    }

    private InvestmentFuturePlanDTO.ComparisonSummaryDTO buildComparisonSummary(
            InvestmentProfileVersion sourceVersion, PortfolioAggregation aggregation) {

        double originalExpectedYield = sourceVersion.getExpectedRoi() != null ? sourceVersion.getExpectedRoi().doubleValue() : 0.0;
        double actualYield = aggregation.totalProfitPercentage();
        double delta = actualYield - originalExpectedYield;

        String prompt = "Kế hoạch gốc kỳ vọng ROI: " + originalExpectedYield + "%. Kết quả thực tế: " + actualYield +
                "% (chênh lệch " + delta + "%). Dòng tiền thuần/tháng: " + aggregation.totalMonthlyNetCashflow() + " VNĐ.\n" +
                "Viết 2 đoạn tiếng Việt ngắn gọn: (1) nhận xét hiệu quả đầu tư, (2) khuyến nghị hành động tiếp theo.\n" +
                "Trả JSON: {\"aiComparisonNote\": \"...\", \"aiActionRecommendation\": \"...\"}";

        String json = "{}";
        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .responseSchema(Schema.builder().type("OBJECT").properties(Map.of(
                            "aiComparisonNote", Schema.builder().type("STRING").build(),
                            "aiActionRecommendation", Schema.builder().type("STRING").build()
                    )).required(List.of("aiComparisonNote", "aiActionRecommendation")).build())
                    .build();
            json = geminiClient.models.generateContent("gemini-2.5-flash", prompt, config).text().trim();
        } catch (Exception e) {
            log.warn("AI comparison fallback: {}", e.getMessage());
        }

        Map<String, String> parsed;
        try {
            parsed = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            parsed = Map.of("aiComparisonNote", "Đang tổng hợp phân tích.", "aiActionRecommendation", "Vui lòng xem chi tiết kết quả bên dưới.");
        }

        return InvestmentFuturePlanDTO.ComparisonSummaryDTO.builder()
                .originalExpectedYield(originalExpectedYield)
                .actualCalculatedYield(Math.round(actualYield * 100.0) / 100.0)
                .yieldDelta(Math.round(delta * 100.0) / 100.0)
                .aiComparisonNote(parsed.getOrDefault("aiComparisonNote", ""))
                .aiActionRecommendation(parsed.getOrDefault("aiActionRecommendation", ""))
                .build();
    }
}
