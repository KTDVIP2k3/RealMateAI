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
 * Future Plan — ĐÃ TÁCH HOÀN TOÀN khỏi InvestmentProfileVersion (thiết kế cũ
 * dùng version="FUTURE_PLAN" đã bỏ). Giờ dùng 5 bảng RIÊNG:
 *   FutureInvestmentPlan -> FutureInvestmentScenario / FutureExecutionPlan /
 *   FutureInvestmentPortfolio -> FuturePortfolioAllocationProperty.
 *
 * Vì sao tách:
 *  - "sourceVersion" (field kiểu InvestmentProfileVersion) chỉ có thể trỏ tới
 *    1 version BÌNH THƯỜNG — InvestmentProfileVersion không còn đại diện cho
 *    future-plan nữa, nên KHÔNG THỂ tạo future TỪ 1 future khác — ràng buộc
 *    này giờ đúng NGAY Ở TẦNG THIẾT KẾ DỮ LIỆU, không cần check thủ công.
 *  - Version gốc KHÔNG CÒN bị deactivate khi tạo future-plan nữa (khác thiết
 *    kế cũ) — vì future-plan giờ không phải "1 version cạnh tranh vị trí
 *    active" trong investment_profile_version nữa, mà là 1 bản ghi con độc
 *    lập treo bên cạnh, profile vẫn giữ nguyên version đang active.
 *
 * FIX 2 bug đã phát hiện so với thiết kế cũ:
 *  (1) InvestmentScenario cũ chỉ lưu 3/8 field AI trả về (thiếu
 *      monthlyCashflow/probability/durationMonths/priceGrowthMin/Max) → GET
 *      lại toàn null. FutureInvestmentScenario lưu ĐỦ cả 8 field.
 *  (2) propertyTypeName bị gán NHẦM bằng tên Portfolio (vd "Tăng trưởng")
 *      thay vì tên PropertyType thật của từng property — giờ group đúng
 *      theo property.propertyType.name.
 *  (3) resolveProperty() trả null bị bỏ qua ÂM THẦM (property biến mất không
 *      dấu vết) — giờ mọi item bị bỏ qua đều được ghi vào "skippedItems" trả
 *      về ngay trong response tạo, không còn im lặng.
 */
@Slf4j
@Service
public class InvestmentFuturePlanServiceImplement implements InvestmentFuturePlanServiceInterface {

    /** Sentinel key nhóm các property KHÔNG có portfolioId — không phải giá trị portfolioId thật nào (portfolio_id thật luôn > 0). */
    private static final Integer UNCLASSIFIED_PORTFOLIO_KEY = -1;

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

    // ── Bảng RIÊNG cho Future Plan (tách hoàn toàn khỏi investment_profile_version) ──
    @Autowired
    private FutureInvestmentPlanRepository futureInvestmentPlanRepository;

    @Autowired
    private FutureInvestmentScenarioRepository futureInvestmentScenarioRepository;

    @Autowired
    private FutureExecutionPlanRepository futureExecutionPlanRepository;

    @Autowired
    private FutureInvestmentPortfolioRepository futureInvestmentPortfolioRepository;

    @Autowired
    private FuturePortfolioAllocationPropertyRepository futurePortfolioAllocationPropertyRepository;

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

            List<String> skippedItems = new ArrayList<>();

            // 1. Tính lợi nhuận từng property (thuần Java, không cần AI vì đây là phép toán xác định)
            List<PropertyProfitResultDTO> profitResults = calculatePropertyProfits(request.getSelectedProperties(), skippedItems);
            PortfolioAggregation aggregation = aggregate(profitResults);

            // 2. Gọi AI sinh scenarios + executionPlan dựa trên số liệu THỰC TẾ vừa tính
            InvestmentPlanDTO aiOutput = callGeminiForFutureScenarios(sourceVersion, profitResults, aggregation);

            // 3. Gọi AI sinh nhận xét so sánh ngắn gọn
            InvestmentFuturePlanDTO.ComparisonSummaryDTO comparison = buildComparisonSummary(sourceVersion, aggregation);

            // 4. Đếm số future-plan ĐÃ CÓ của CÙNG investment profile để tự sinh tên
            //    "Kết quả dự đoán N" — KHÔNG còn deactivate version gốc nữa (future-plan
            //    giờ là bản ghi con độc lập, không cạnh tranh vị trí "active" của profile).
            InvestmentProfile profile = sourceVersion.getInvestmentProfile();
            LocalDateTime now = LocalDateTime.now();
            long existingCount = profile != null
                    ? futureInvestmentPlanRepository.countByInvestmentProfile_InvestmentProfileId(profile.getInvestmentProfileId())
                    : 0L;
            String autoName = (request.getPlanName() != null && !request.getPlanName().isBlank())
                    ? request.getPlanName()
                    : "Kết quả dự đoán " + (existingCount + 1);

            Map<String, Object> profitSummaryMap = buildProfitSummaryMap(aggregation, comparison, aiOutput.getScore());

            // 5. Tạo FutureInvestmentPlan — clone tham số tài chính từ sourceVersion
            FutureInvestmentPlan newPlan = FutureInvestmentPlan.builder()
                    .investmentProfile(profile)
                    .sourceVersion(sourceVersion)
                    .name(autoName)
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
                    .profitSummary(profitSummaryMap)
                    .isActive(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            FutureInvestmentPlan savedPlan = futureInvestmentPlanRepository.save(newPlan);

            // 6. Lưu scenarios từ AI output — LƯU ĐỦ CẢ 8 FIELD (fix bug null trước đây
            //    chỉ lưu 3/8 field khiến GET lại thiếu chi tiết lợi nhuận đã add vào)
            if (aiOutput.getScenarios() != null) {
                for (InvestmentScenarioDTO s : aiOutput.getScenarios()) {
                    futureInvestmentScenarioRepository.save(FutureInvestmentScenario.builder()
                            .futureInvestmentPlan(savedPlan)
                            .name(s.getEnumScenarioType())
                            .scenarioType(s.getEnumScenarioType())
                            .expectedReturnRate(s.getDecimprofitYield())
                            .monthlyCashflow(s.getDecimmonthlyCashflow())
                            .probability(s.getDecimprobability())
                            .durationMonths(s.getDurationMonths())
                            .priceGrowthMin(s.getDecimpriceGrowthMin())
                            .priceGrowthMax(s.getDecimpriceGrowthMax())
                            .description(s.getTextMarketNote())
                            .isActive(true)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                }
            }

            // 7. Lưu execution plan
            if (aiOutput.getExecutionPlan() != null) {
                String descJson = objectMapper.writeValueAsString(aiOutput.getExecutionPlan());
                futureExecutionPlanRepository.save(FutureExecutionPlan.builder()
                        .futureInvestmentPlan(savedPlan)
                        .name("AI Future Execution Plan")
                        .description(descJson)
                        .status("ACTIVE")
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }

            // 8. Lưu portfolio + properties — ghi TRỰC TIẾP vào
            //    FuturePortfolioAllocationProperty (bỏ bảng trung gian, xem javadoc
            //    FutureInvestmentPortfolio). Group theo portfolioId investor gửi lên.
            //
            //    FIX quan trọng: TRƯỚC ĐÂY property thiếu portfolioId (thường gặp
            //    với property MANUAL) bị LỌC BỎ HOÀN TOÀN ở bước này — dù vẫn được
            //    tính vào phân tích lợi nhuận tổng ở calculatePropertyProfits() phía
            //    trên (chạy trên TOÀN BỘ selectedProperties, không lọc theo
            //    portfolioId) — gây ra tình trạng "vẫn phân tích nhưng không hiện
            //    trong danh sách". Giờ dùng sentinel key UNCLASSIFIED_KEY để nhóm
            //    NHỮNG property này vào 1 FutureInvestmentPortfolio riêng với
            //    portfolio=null ("Chưa phân loại") — KHÔNG BAO GIỜ bị mất nữa.
            Map<Integer, List<GenerateFuturePlanRequest.SelectedPropertyItem>> byPortfolio = request.getSelectedProperties()
                    .stream()
                    .collect(Collectors.groupingBy(item ->
                            item.getPortfolioId() != null ? item.getPortfolioId() : UNCLASSIFIED_PORTFOLIO_KEY));

            for (Map.Entry<Integer, List<GenerateFuturePlanRequest.SelectedPropertyItem>> entry : byPortfolio.entrySet()) {
                boolean isUnclassified = UNCLASSIFIED_PORTFOLIO_KEY.equals(entry.getKey());
                Portfolio portfolio = null;
                if (!isUnclassified) {
                    portfolio = portfolioRepository.findById(entry.getKey()).orElse(null);
                    if (portfolio == null) {
                        skippedItems.add("portfolioId=" + entry.getKey() + ": không tồn tại, " + entry.getValue().size()
                                + " property được chuyển sang nhóm \"Chưa phân loại\" thay vì bỏ qua");
                    }
                } else {
                    skippedItems.add(entry.getValue().size() + " property không có portfolioId — đã lưu vào nhóm \"Chưa phân loại\" (vẫn hiện trong danh sách, chỉ không thuộc danh mục Tăng trưởng/Thanh khoản cụ thể)");
                }

                List<GenerateFuturePlanRequest.SelectedPropertyItem> items = entry.getValue();
                long capitalForThisPortfolio = items.stream()
                        .mapToLong(i -> i.getActualPurchasePrice() != null ? i.getActualPurchasePrice() : 0L)
                        .sum();

                FutureInvestmentPortfolio futurePortfolio = futureInvestmentPortfolioRepository.save(
                        FutureInvestmentPortfolio.builder()
                                .futureInvestmentPlan(savedPlan)
                                .portfolio(portfolio) // null khi "Chưa phân loại" hoặc portfolioId không tồn tại
                                .percentage(0) // future plan không tính lại % phân bổ AI, giữ 0 vì capital đã thực
                                .capital(capitalForThisPortfolio)
                                .isActive(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build());

                for (GenerateFuturePlanRequest.SelectedPropertyItem item : items) {
                    Property property = resolveProperty(item);
                    if (property == null) {
                        skippedItems.add(describeItem(item) + ": không tìm thấy property (kiểm tra lại listingId/manualPropertyId), bỏ qua");
                        continue;
                    }

                    futurePortfolioAllocationPropertyRepository.save(
                            FuturePortfolioAllocationProperty.builder()
                                    .futureInvestmentPortfolio(futurePortfolio)
                                    .property(property)
                                    .propertySource(item.getPropertySource() != null ? item.getPropertySource() : "SYSTEM")
                                    .usagePurpose(item.getUsagePurpose())
                                    .monthlyRevenue(item.getMonthlyRevenue())
                                    .monthlyOperatingCost(item.getMonthlyOperatingCost())
                                    .actualPurchasePrice(item.getActualPurchasePrice())
                                    .evaluatedMarketPrice(item.getEvaluatedMarketPrice())
                                    .holdingMonths(resolveHoldingMonths(item.getHoldingMonths()))
                                    .isActive(true)
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .build());
                }
            }

            if (!skippedItems.isEmpty()) {
                log.warn("[InvestmentFuturePlanService] generateAndSaveFuturePlan: futurePlanId={}, {} item bị bỏ qua: {}",
                        savedPlan.getFutureInvestmentPlanId(), skippedItems.size(), skippedItems);
            }

            // 9. Response GỌN — chỉ xác nhận đã tạo + newVersionId (= futureInvestmentPlanId)
            //    để FE gọi GET /investment-plans/future/{newVersionId} lấy output đầy đủ.
            FuturePlanCreatedResponse responseDTO = FuturePlanCreatedResponse.builder()
                    .newVersionId(savedPlan.getFutureInvestmentPlanId())
                    .newVersionName(savedPlan.getName())
                    .sourceVersionId(sourceVersion.getProfileVersionId())
                    .sourceVersionName(sourceVersion.getProfileVersionName())
                    .createdAt(savedPlan.getCreatedAt())
                    .skippedItems(skippedItems)
                    .build();

            String msg = skippedItems.isEmpty()
                    ? "Tạo và lưu kế hoạch tương lai thành công"
                    : "Tạo kế hoạch tương lai thành công, nhưng có " + skippedItems.size() + " property bị bỏ qua — xem chi tiết trong \"skippedItems\"";

            return ResponseEntity.ok(ApiResponse.success(responseDTO, msg));

        } catch (Exception e) {
            log.error("Error in generateAndSaveFuturePlan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }

    /** Mô tả ngắn 1 item cho mục đích log/skippedItems (không lộ toàn bộ payload, chỉ đủ để investor/dev nhận diện). */
    private static String describeItem(GenerateFuturePlanRequest.SelectedPropertyItem item) {
        if (item.getListingId() != null) return "listingId=" + item.getListingId();
        if (item.getManualPropertyId() != null) return "manualPropertyId=" + item.getManualPropertyId();
        return "property không xác định";
    }

    // =====================================================================
    // GET DETAIL — đọc lại từ FutureInvestmentPlan (bảng RIÊNG), không còn
    // đọc từ InvestmentProfileVersion nữa.
    // =====================================================================

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> getFuturePlanDetail(Integer futurePlanId) {
        try {
            FutureInvestmentPlan plan = futureInvestmentPlanRepository.findById(futurePlanId).orElse(null);
            if (plan == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Version_Not_Found", "Không tìm thấy kế hoạch tương lai: " + futurePlanId));
            }

            InvestmentProfileVersion sourceVersion = plan.getSourceVersion();

            // Scenarios — đọc lại ĐỦ CẢ 8 field (fix bug null trước đây)
            List<InvestmentScenarioDTO> scenarioDTOs = futureInvestmentScenarioRepository
                    .findByFutureInvestmentPlan_FutureInvestmentPlanId(futurePlanId).stream()
                    .map(s -> InvestmentScenarioDTO.builder()
                            .enumScenarioType(s.getScenarioType())
                            .decimprofitYield(s.getExpectedReturnRate())
                            .decimmonthlyCashflow(s.getMonthlyCashflow())
                            .decimprobability(s.getProbability())
                            .durationMonths(s.getDurationMonths())
                            .decimpriceGrowthMin(s.getPriceGrowthMin())
                            .decimpriceGrowthMax(s.getPriceGrowthMax())
                            .textMarketNote(s.getDescription())
                            .build())
                    .collect(Collectors.toList());

            // Execution plan
            ExecutionPlanDTO executionPlanDTO = null;
            List<FutureExecutionPlan> execPlans = futureExecutionPlanRepository
                    .findByFutureInvestmentPlan_FutureInvestmentPlanId(futurePlanId);
            if (!execPlans.isEmpty() && execPlans.get(0).getDescription() != null) {
                executionPlanDTO = objectMapper.readValue(execPlans.get(0).getDescription(), ExecutionPlanDTO.class);
            }

            // Đọc trực tiếp profitSummary đã lưu sẵn ở cấp plan — không cần tính lại
            Map<String, Object> profitSummary = plan.getProfitSummary() != null ? plan.getProfitSummary() : Map.of();

            // ── Tái dựng investmentPortfolios + propertyProfitResults từ
            // FutureInvestmentPortfolio -> FuturePortfolioAllocationProperty đã lưu
            // lúc tạo. Lợi nhuận từng property TÍNH LẠI bằng đúng computeProfitResult()
            // (dùng chung công thức với lúc tạo).
            //
            // FIX bug propertyTypeName: trước đây gán NHẦM bằng tên Portfolio (vd
            // "Tăng trưởng") cho MỌI property trong 1 allocation — giờ GROUP đúng các
            // property theo property.propertyType.name thật của từng property, mỗi
            // nhóm property-type ra 1 PortfolioAllocationDTO riêng (đúng ý nghĩa gốc
            // của field "propertyTypeName" — xem PortfolioAllocationDTO).
            List<InvestmentPortfolioDTO> investmentPortfolios = new ArrayList<>();
            List<PropertyProfitResultDTO> propertyProfitResults = new ArrayList<>();

            List<FutureInvestmentPortfolio> portfolios = futureInvestmentPortfolioRepository
                    .findByFutureInvestmentPlan_FutureInvestmentPlanId(futurePlanId);

            for (FutureInvestmentPortfolio fip : portfolios) {
                List<FuturePortfolioAllocationProperty> paps = fip.getFuturePortfolioAllocationProperties() != null
                        ? fip.getFuturePortfolioAllocationProperties() : List.of();

                // Group theo property.propertyType.name (fix bug — trước đây KHÔNG group,
                // gán cứng 1 propertyTypeName sai cho toàn bộ properties trong portfolio)
                Map<String, List<FuturePortfolioAllocationProperty>> byPropertyType = paps.stream()
                        .collect(Collectors.groupingBy(pap -> {
                            Property p = pap.getProperty();
                            return (p != null && p.getPropertyType() != null && p.getPropertyType().getName() != null)
                                    ? p.getPropertyType().getName() : "Khác";
                        }, LinkedHashMap::new, Collectors.toList()));

                List<PortfolioAllocationDTO> allocationDTOs = new ArrayList<>();
                for (Map.Entry<String, List<FuturePortfolioAllocationProperty>> group : byPropertyType.entrySet()) {
                    List<PortfolioAllocationPropertyDTO> propertyDTOs = new ArrayList<>();
                    for (FuturePortfolioAllocationProperty pap : group.getValue()) {
                        Property property = pap.getProperty();
                        String propertyName = property != null && property.getTitle() != null
                                ? property.getTitle() : "Bất động sản";

                        propertyDTOs.add(PortfolioAllocationPropertyDTO.builder()
                                .portfolioAllocationPropertyId(pap.getFuturePortfolioAllocationPropertyId())
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

                            propertyProfitResults.add(computeProfitResult(
                                    property != null ? property.getPropertyId() : null,
                                    propertyName, pap.getUsagePurpose(),
                                    initialPrice, evaluatedPrice, monthlyRevenue, monthlyOpCost, holdingMonths));
                        }
                    }

                    allocationDTOs.add(PortfolioAllocationDTO.builder()
                            .propertyTypeName(group.getKey())
                            .properties(propertyDTOs)
                            .build());
                }

                investmentPortfolios.add(InvestmentPortfolioDTO.builder()
                        .portfolioId(fip.getPortfolio() != null ? fip.getPortfolio().getPortfolioId() : null)
                        .portfolioName(fip.getPortfolio() != null ? fip.getPortfolio().getName() : "Chưa phân loại")
                        .percentage(fip.getPercentage())
                        .capital(fip.getCapital() != null ? fip.getCapital().doubleValue() : 0.0)
                        .allocations(allocationDTOs)
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
                    .newVersionId(plan.getFutureInvestmentPlanId())
                    .newVersionName(plan.getName())
                    .sourceVersionId(sourceVersion != null ? sourceVersion.getProfileVersionId() : null)
                    .sourceVersionName(sourceVersion != null ? sourceVersion.getProfileVersionName() : null)
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
    // GET /investment-plans/future/by-source/{sourceVersionId} — danh sách
    // TÓM TẮT các future-plan phái sinh từ 1 version gốc.
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

            List<FutureInvestmentPlan> futurePlans = futureInvestmentPlanRepository
                    .findBySourceVersion_ProfileVersionIdOrderByCreatedAtDesc(sourceVersionId);

            List<FutureVersionSummaryDTO> result = futurePlans.stream()
                    .map(p -> {
                        Map<String, Object> summary = p.getProfitSummary() != null ? p.getProfitSummary() : Map.of();
                        return FutureVersionSummaryDTO.builder()
                                .futureVersionId(p.getFutureInvestmentPlanId())
                                .futureVersionName(p.getName())
                                .sourceVersionId(sourceVersionId)
                                .actualCalculatedYield(toDoubleOrNull(summary.get("totalProfitPercentage")))
                                .yieldDelta(toDoubleOrNull(summary.get("yieldDelta")))
                                .portfolioScore(toIntegerOrNull(summary.get("portfolioScore")))
                                .isActive(p.getIsActive())
                                .createdAt(p.getCreatedAt())
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
     *  (2) getFuturePlanDetail() lúc ĐỌC LẠI (input từ FuturePortfolioAllocationProperty đã lưu).
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

    /**
     * doanh thu tháng (monthlyRevenue) / chi phí tháng (monthlyOperatingCost) CÓ THỂ
     * NULL (investor chưa xác định, ví dụ property mua để ở/chưa cho thuê) — mặc
     * định về 0 khi tính, KHÔNG bắt buộc nhập.
     */
    private List<PropertyProfitResultDTO> calculatePropertyProfits(
            List<GenerateFuturePlanRequest.SelectedPropertyItem> items, List<String> skippedOut) {
        List<PropertyProfitResultDTO> results = new ArrayList<>();
        for (GenerateFuturePlanRequest.SelectedPropertyItem item : items) {
            if (item.getActualPurchasePrice() == null || item.getActualPurchasePrice() <= 0) {
                skippedOut.add(describeItem(item) + ": thiếu actualPurchasePrice (hoặc <= 0), không tính lợi nhuận cho property này");
                continue;
            }

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
