package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CriteriaRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.GenerateFuturePlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestmentPlanRequest;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
public class InvestmentFuturePlanServiceImplement implements InvestmentFuturePlanServiceInterface {

    @Autowired
    private AuthenUntil authenUntil;

    @Autowired
    private InvestmentProfileVersionRepository investmentProfileVersionRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private StrategyRepository strategyRepository;

    @Autowired
    private FutureInvestmentPlanRepository futureInvestmentPlanRepository;

    // MỚI: dùng CHUNG bean hạ tầng Client Gemini (KHÔNG gọi lại business logic
    // của InvestmentPlanServiceImplement) — đúng ý "tách riêng biệt".
    @Autowired
    private Client geminiClient;

    @Autowired
    private ObjectMapper objectMapper;

    // =====================================================================
    // GENERATE + SAVE — 1 transaction duy nhất
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
                        .body(ApiResponse.fail("No_Properties", "Vui lòng chọn ít nhất 1 bất động sản đã mua để phân tích."));
            }

            // ── BƯỚC 1 — Tra lại đúng ProposedProperty GỐC (kết quả Investment
            // Plan) tương ứng mỗi listingId investor phản hồi, gộp toàn bộ
            // ProposedProperty của sourceVersion vào 1 Map để tra O(1). ──
            Map<Integer, ProposedProperty> plannedByListingId = new HashMap<>();
            if (sourceVersion.getInvestmentCriterias() != null) {
                for (InvestmentCriteria c : sourceVersion.getInvestmentCriterias()) {
                    if (c.getProposedProperties() == null) continue;
                    for (ProposedProperty p : c.getProposedProperties()) {
                        if (p.getListingId() != null) plannedByListingId.put(p.getListingId(), p);
                    }
                }
            }

            // ── BƯỚC 2 — Tính DÒNG TIỀN THỰC TẾ (Java thuần, KHÔNG AI, KHÔNG
            // cần giá thẩm định) cho từng property, đối chiếu với kế hoạch gốc. ──
            List<String> skippedItems = new ArrayList<>();
            List<PropertyFutureAnalysisDTO> analysisResults = new ArrayList<>();

            for (GenerateFuturePlanRequest.SelectedPropertyItem item : request.getSelectedProperties()) {
                if (item.getActualPurchasePrice() == null || item.getActualPurchasePrice() <= 0) {
                    skippedItems.add(describeItem(item) + ": thiếu actualPurchasePrice (hoặc <= 0), không phân tích được property này");
                    continue;
                }

                ProposedProperty planned = item.getListingId() != null ? plannedByListingId.get(item.getListingId()) : null;
                if (planned == null) {
                    skippedItems.add(describeItem(item) + ": không tìm thấy trong kết quả Investment Plan gốc (có thể propertySource=MANUAL) — vẫn phân tích nhưng KHÔNG có baseline kế hoạch để so sánh");
                }

                String propertyName = resolvePropertyName(item, planned);

                long actualMonthlyRevenue = item.getMonthlyRevenue() != null ? item.getMonthlyRevenue() : 0L;
                long actualMonthlyOperatingCost = item.getMonthlyOperatingCost() != null ? item.getMonthlyOperatingCost() : 0L;
                long actualMonthlyNetCashflow = actualMonthlyRevenue - actualMonthlyOperatingCost;
                int actualHoldingMonths = item.getHoldingMonths() != null && item.getHoldingMonths() > 0 ? item.getHoldingMonths() : 1;

                double actualAnnualYield = ((double) actualMonthlyNetCashflow * 12 / item.getActualPurchasePrice()) * 100.0;

                Long plannedNetCashflow = planned != null ? planned.getNetCashflow() : null;
                Long monthlyDelta = plannedNetCashflow != null ? (actualMonthlyNetCashflow - plannedNetCashflow) : null;

                analysisResults.add(PropertyFutureAnalysisDTO.builder()
                        .listingId(item.getListingId())
                        .propertyProjectName(propertyName)
                        .plannedMonthlyRentalCashflow(planned != null ? planned.getMonthlyRentalCashflow() : null)
                        .plannedNetCashflow(plannedNetCashflow)
                        .plannedRoiPercentage(planned != null ? planned.getRoiPercentage() : null)
                        .plannedEstimatedProfit(planned != null ? planned.getEstimatedProfit() : null)
                        .usagePurpose(item.getUsagePurpose())
                        .actualPurchasePrice(item.getActualPurchasePrice())
                        .actualMonthlyRevenue(actualMonthlyRevenue)
                        .actualMonthlyOperatingCost(actualMonthlyOperatingCost)
                        .actualMonthlyNetCashflow(actualMonthlyNetCashflow)
                        .actualHoldingMonths(actualHoldingMonths)
                        .actualAnnualCashflowYieldPercentage(Math.round(actualAnnualYield * 100.0) / 100.0)
                        .monthlyCashflowDelta(monthlyDelta)
                        .build());
            }

            if (analysisResults.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("No_Valid_Properties", "Không có property nào đủ dữ liệu để phân tích. Chi tiết: " + skippedItems));
            }

            // ── BƯỚC 3 — Effective values (bối cảnh tài chính hiện tại) — ưu
            // tiên input mới investor nhập, fallback sourceVersion nếu để trống. ──
            Long effEquity = request.getEquity() != null ? request.getEquity() : sourceVersion.getEquity();
            Long effLoanCapital = request.getLoanCapital() != null ? request.getLoanCapital() : sourceVersion.getLoanCapital();
            Long effCurrentCashFlow = request.getCurrentCashFlow() != null ? request.getCurrentCashFlow() : sourceVersion.getCurrentCashflow();
            String effConscious = request.getConsciousName() != null ? request.getConsciousName() : sourceVersion.getConscious();
            List<String> effWardNames = request.getWardNames() != null ? request.getWardNames() : sourceVersion.getWards();
            Integer effLongTermYear = request.getLongTermYear() != null ? request.getLongTermYear() : sourceVersion.getLongTermYear();
            Map<String, Object> effInvestmentStrategyDetail = request.getInvestmentStrategyDetail() != null
                    ? request.getInvestmentStrategyDetail() : sourceVersion.getInvestmentStrategyDetail();

            Strategy effStrategy = sourceVersion.getStrategy();
            if (request.getStrategyId() != null) {
                Strategy requested = strategyRepository.findById(request.getStrategyId()).orElse(null);
                if (requested != null) effStrategy = requested;
                else skippedItems.add("strategyId=" + request.getStrategyId() + ": không tồn tại, giữ nguyên strategy của sourceVersion");
            }

            // ── BƯỚC 4 — Gọi AI PHÂN TÍCH (1 lần gọi DUY NHẤT, RIÊNG BIỆT khỏi
            // Investment Plan) — AI CHỈ viết nhận xét/khuyến nghị định tính +
            // 1 điểm số tổng thể, KHÔNG tính lại số liệu (đã tính ở Bước 2). ──
            AIAnalysisResult aiResult;
            try {
                aiResult = callAIForFutureAnalysis(analysisResults, effEquity, effLoanCapital, effCurrentCashFlow,
                        effConscious, effWardNames, effLongTermYear, effStrategy, effInvestmentStrategyDetail);
            } catch (Exception e) {
                log.warn("[InvestmentFuturePlanService] AI phân tích lỗi, dùng fallback rule-based: {}", e.getMessage());
                aiResult = buildFallbackAnalysis(analysisResults);
                skippedItems.add("AI phân tích tạm thời không khả dụng, đã dùng nhận xét mặc định (rule-based): " + e.getMessage());
            }

            // Gán nhận xét AI vào từng property (map theo listingId, giữ đúng thứ tự analysisResults).
            Map<Integer, AIPropertyNote> noteByListingId = aiResult.propertyNotes.stream()
                    .filter(n -> n.listingId != null)
                    .collect(Collectors.toMap(n -> n.listingId, n -> n, (a, b) -> a));
            List<PropertyFutureAnalysisDTO> finalResults = new ArrayList<>();
            for (PropertyFutureAnalysisDTO r : analysisResults) {
                AIPropertyNote note = r.getListingId() != null ? noteByListingId.get(r.getListingId()) : null;
                finalResults.add(r.toBuilder()
                        .aiAnalysisNote(note != null ? note.analysisNote : "Không có đủ dữ liệu để AI phân tích riêng cho property này.")
                        .aiActionRecommendation(note != null ? note.actionRecommendation : "Cần bổ sung thêm dữ liệu thực tế trước khi đưa ra khuyến nghị.")
                        .build());
            }

            // ── BƯỚC 5 — Tổng hợp toàn danh mục (Java tính) ──
            long totalPlanned = finalResults.stream().filter(r -> r.getPlannedNetCashflow() != null)
                    .mapToLong(PropertyFutureAnalysisDTO::getPlannedNetCashflow).sum();
            long totalActual = finalResults.stream().mapToLong(PropertyFutureAnalysisDTO::getActualMonthlyNetCashflow).sum();
            long totalInvested = finalResults.stream().mapToLong(PropertyFutureAnalysisDTO::getActualPurchasePrice).sum();

            InvestmentProfile profile = sourceVersion.getInvestmentProfile();
            LocalDateTime now = LocalDateTime.now();
            long existingCount = profile != null
                    ? futureInvestmentPlanRepository.countByInvestmentProfile_InvestmentProfileId(profile.getInvestmentProfileId())
                    : 0L;
            String autoName = (request.getPlanName() != null && !request.getPlanName().isBlank())
                    ? request.getPlanName() : "Kết quả dự đoán " + (existingCount + 1);

            // ── BƯỚC 6 — Lưu snapshot JSON đầy đủ (đọc lại nguyên vẹn khi GET). ──
            Map<String, Object> analysisSnapshot = new LinkedHashMap<>();
            analysisSnapshot.put("propertyAnalysisResults", finalResults.stream().map(this::toMap).collect(Collectors.toList()));
            analysisSnapshot.put("totalPlannedMonthlyNetCashflow", totalPlanned);
            analysisSnapshot.put("totalActualMonthlyNetCashflow", totalActual);
            analysisSnapshot.put("totalMonthlyCashflowDelta", totalActual - totalPlanned);
            analysisSnapshot.put("totalActualInvestedCapital", totalInvested);
            analysisSnapshot.put("aiOverallScore", aiResult.overallScore);
            analysisSnapshot.put("aiOverallNote", aiResult.overallNote);
            analysisSnapshot.put("aiOverallRecommendation", aiResult.overallRecommendation);

            FutureInvestmentPlan newPlan = FutureInvestmentPlan.builder()
                    .investmentProfile(profile)
                    .sourceVersion(sourceVersion)
                    .name(autoName)
                    .strategy(effStrategy)
                    .equity(effEquity)
                    .loanCapital(effLoanCapital)
                    .currentCashflow(effCurrentCashFlow)
                    .conscious(effConscious)
                    .wards(effWardNames)
                    .longTermYear(effLongTermYear)
                    .investmentStrategyDetail(effInvestmentStrategyDetail)
                    .analysisSnapshot(analysisSnapshot)
                    .isActive(true)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            FutureInvestmentPlan savedPlan = futureInvestmentPlanRepository.save(newPlan);

            FuturePlanCreatedResponse response = FuturePlanCreatedResponse.builder()
                    .futureInvestmentPlanId(savedPlan.getFutureInvestmentPlanId())
                    .futureInvestmentPlanName(savedPlan.getName())
                    .sourceVersionId(sourceVersion.getProfileVersionId())
                    .sourceVersionName(sourceVersion.getProfileVersionName())
                    .createdAt(now)
                    .skippedItems(skippedItems)
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response, "Tạo kế hoạch tương lai thành công"));

        } catch (Exception e) {
            log.error("[InvestmentFuturePlanService] generateAndSaveFuturePlan lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    // =====================================================================
    // GET DETAIL — đọc lại nguyên vẹn từ analysisSnapshot JSON, KHÔNG tính
    // lại / KHÔNG gọi lại AI.
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
            Map<String, Object> snapshot = plan.getAnalysisSnapshot() != null ? plan.getAnalysisSnapshot() : Map.of();

            List<PropertyFutureAnalysisDTO> results = new ArrayList<>();
            Object rawList = snapshot.get("propertyAnalysisResults");
            if (rawList instanceof List) {
                for (Object o : (List<?>) rawList) {
                    results.add(mapToPropertyFutureAnalysisDTO((Map<String, Object>) o));
                }
            }

            InvestmentFuturePlanDTO dto = InvestmentFuturePlanDTO.builder()
                    .futureInvestmentPlanId(plan.getFutureInvestmentPlanId())
                    .futureInvestmentPlanName(plan.getName())
                    .sourceVersionId(sourceVersion != null ? sourceVersion.getProfileVersionId() : null)
                    .sourceVersionName(sourceVersion != null ? sourceVersion.getProfileVersionName() : null)
                    .equity(plan.getEquity())
                    .loanCapital(plan.getLoanCapital())
                    .currentCashFlow(plan.getCurrentCashflow())
                    .consciousName(plan.getConscious())
                    .wardNames(plan.getWards())
                    .longTermYear(plan.getLongTermYear())
                    .strategyId(plan.getStrategy() != null ? plan.getStrategy().getStrategyId() : null)
                    .strategyName(plan.getStrategy() != null ? plan.getStrategy().getName() : null)
                    .investmentStrategyDetail(plan.getInvestmentStrategyDetail())
                    .propertyAnalysisResults(results)
                    .totalPlannedMonthlyNetCashflow(toLongOrNull(snapshot.get("totalPlannedMonthlyNetCashflow")))
                    .totalActualMonthlyNetCashflow(toLongOrNull(snapshot.get("totalActualMonthlyNetCashflow")))
                    .totalMonthlyCashflowDelta(toLongOrNull(snapshot.get("totalMonthlyCashflowDelta")))
                    .totalActualInvestedCapital(toLongOrNull(snapshot.get("totalActualInvestedCapital")))
                    .aiOverallScore(toIntegerOrNull(snapshot.get("aiOverallScore")))
                    .aiOverallNote((String) snapshot.get("aiOverallNote"))
                    .aiOverallRecommendation((String) snapshot.get("aiOverallRecommendation"))
                    .build();

            return ResponseEntity.ok(ApiResponse.success(dto, "Lấy chi tiết kế hoạch tương lai thành công"));

        } catch (Exception e) {
            log.error("[InvestmentFuturePlanService] getFuturePlanDetail lỗi", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getFutureVersionsBySourceVersionId(Integer sourceVersionId) {
        try {
            InvestmentProfileVersion sourceVersion = investmentProfileVersionRepository.findById(sourceVersionId).orElse(null);
            if (sourceVersion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Version_Not_Found", "Không tìm thấy phiên bản gốc với ID: " + sourceVersionId));
            }

            List<FutureInvestmentPlan> futurePlans = futureInvestmentPlanRepository
                    .findBySourceVersion_ProfileVersionIdOrderByCreatedAtDesc(sourceVersionId);

            List<FutureVersionSummaryDTO> result = futurePlans.stream()
                    .map(p -> {
                        Map<String, Object> snap = p.getAnalysisSnapshot() != null ? p.getAnalysisSnapshot() : Map.of();
                        return FutureVersionSummaryDTO.builder()
                                .futureVersionId(p.getFutureInvestmentPlanId())
                                .futureVersionName(p.getName())
                                .sourceVersionId(sourceVersionId)
                                .portfolioScore(toIntegerOrNull(snap.get("aiOverallScore")))
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

    // =====================================================================
    // AI CALL — RIÊNG BIỆT, ĐỘC LẬP với Investment Plan. AI CHỈ viết nhận
    // xét/khuyến nghị định tính + 1 điểm số, KHÔNG tính toán số liệu.
    // =====================================================================

    private static class AIPropertyNote {
        Integer listingId;
        String analysisNote;
        String actionRecommendation;
    }

    private static class AIAnalysisResult {
        Integer overallScore;
        String overallNote;
        String overallRecommendation;
        List<AIPropertyNote> propertyNotes = new ArrayList<>();
    }

    private AIAnalysisResult callAIForFutureAnalysis(
            List<PropertyFutureAnalysisDTO> results, Long equity, Long loanCapital, Long currentCashFlow,
            String consciousName, List<String> wardNames, Integer longTermYear, Strategy strategy,
            Map<String, Object> investmentStrategyDetail) throws Exception {

        Map<String, Object> currentContext = new LinkedHashMap<>();
        currentContext.put("equity", equity);
        currentContext.put("loanCapital", loanCapital);
        currentContext.put("currentCashFlow", currentCashFlow);
        currentContext.put("consciousName", consciousName);
        currentContext.put("wardNames", wardNames);
        currentContext.put("longTermYear", longTermYear);
        currentContext.put("strategyName", strategy != null ? strategy.getName() : null);
        currentContext.put("investmentStrategyDetail", investmentStrategyDetail);

        List<Map<String, Object>> propertiesForAi = new ArrayList<>();
        for (PropertyFutureAnalysisDTO r : results) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("listingId", r.getListingId());
            pm.put("propertyProjectName", r.getPropertyProjectName());
            pm.put("usagePurpose", r.getUsagePurpose());
            Map<String, Object> planned = new LinkedHashMap<>();
            planned.put("monthlyRentalCashflow", r.getPlannedMonthlyRentalCashflow());
            planned.put("netCashflow", r.getPlannedNetCashflow());
            planned.put("roiPercentage", r.getPlannedRoiPercentage());
            planned.put("estimatedProfit", r.getPlannedEstimatedProfit());
            pm.put("planned", planned);
            Map<String, Object> actual = new LinkedHashMap<>();
            actual.put("purchasePrice", r.getActualPurchasePrice());
            actual.put("monthlyRevenue", r.getActualMonthlyRevenue());
            actual.put("monthlyOperatingCost", r.getActualMonthlyOperatingCost());
            actual.put("monthlyNetCashflow", r.getActualMonthlyNetCashflow());
            actual.put("holdingMonths", r.getActualHoldingMonths());
            actual.put("annualCashflowYieldPercentage", r.getActualAnnualCashflowYieldPercentage());
            actual.put("monthlyCashflowDeltaVsPlanned", r.getMonthlyCashflowDelta());
            pm.put("actual", actual);
            propertiesForAi.add(pm);
        }

        String currentContextJson = objectMapper.writeValueAsString(currentContext);
        String propertiesJson = objectMapper.writeValueAsString(propertiesForAi);

        String prompt = "Bạn là chuyên gia phân tích tài chính bất động sản tại Việt Nam.\n\n"
                + "Nhà đầu tư ĐÃ MUA các bất động sản dưới đây theo 1 kế hoạch đầu tư đã lập trước đó (planned = số liệu AI dự tính khi lập kế hoạch), "
                + "và đã nhập thông tin sử dụng THỰC TẾ sau một thời gian nắm giữ (actual = số liệu thực tế, ĐÃ được tính sẵn, không cần tính lại).\n\n"
                + "Bối cảnh tài chính hiện tại của nhà đầu tư: " + currentContextJson + "\n\n"
                + "Danh sách bất động sản (kế hoạch vs thực tế): " + propertiesJson + "\n\n"
                + "YÊU CẦU:\n"
                + "1. Với MỖI bất động sản, viết 1 câu nhận xét ngắn (analysisNote) so sánh dòng tiền thực tế với kế hoạch, và 1 khuyến nghị hành động cụ thể "
                + "(actionRecommendation): tiếp tục giữ / xem xét bán / tái đầu tư dòng tiền dư / điều chỉnh giá thuê hoặc chi phí vận hành.\n"
                + "2. Đánh giá 1 điểm số TỔNG THỂ (overallScore, 0-100) phản ánh mức độ danh mục đang đi ĐÚNG HƯỚNG so với kế hoạch ban đầu.\n"
                + "3. Viết 1 nhận xét TỔNG THỂ (overallNote) và 1 khuyến nghị TỔNG THỂ (overallRecommendation) cho toàn bộ danh mục.\n"
                + "4. TUYỆT ĐỐI KHÔNG tự tính lại hoặc suy đoán thêm số liệu tài chính nào — chỉ dùng ĐÚNG số liệu đã cho.\n"
                + "5. Toàn bộ văn bản trả lời bằng TIẾNG VIỆT.\n"
                + "6. Trả về đúng 1 object JSON theo schema đã cung cấp.";

        Schema propertyNoteSchema = Schema.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "listingId", Schema.builder().type("INTEGER").build(),
                        "analysisNote", Schema.builder().type("STRING").build(),
                        "actionRecommendation", Schema.builder().type("STRING").build()
                ))
                .required(List.of("analysisNote", "actionRecommendation"))
                .build();

        Schema dataSchema = Schema.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "overallScore", Schema.builder().type("INTEGER").build(),
                        "overallNote", Schema.builder().type("STRING").build(),
                        "overallRecommendation", Schema.builder().type("STRING").build(),
                        "propertyAnalyses", Schema.builder().type("ARRAY").items(propertyNoteSchema).build()
                ))
                .required(List.of("overallScore", "overallNote", "overallRecommendation", "propertyAnalyses"))
                .build();

        Schema rootSchema = Schema.builder()
                .type("OBJECT")
                .properties(Map.of("data", dataSchema))
                .required(List.of("data"))
                .build();

        GenerateContentConfig config = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .responseSchema(rootSchema)
                .build();

        var response = geminiClient.models.generateContent("gemini-2.5-flash", prompt, config);
        String jsonResponse = response.text();
        if (jsonResponse == null || jsonResponse.isBlank()) {
            throw new IllegalStateException("AI không trả về kết quả phân tích.");
        }

        Map<String, Object> parsed = objectMapper.readValue(jsonResponse, Map.class);
        Map<String, Object> data = (Map<String, Object>) parsed.get("data");

        AIAnalysisResult result = new AIAnalysisResult();
        result.overallScore = toIntegerOrNull(data.get("overallScore"));
        result.overallNote = (String) data.get("overallNote");
        result.overallRecommendation = (String) data.get("overallRecommendation");

        Object rawAnalyses = data.get("propertyAnalyses");
        if (rawAnalyses instanceof List) {
            for (Object o : (List<?>) rawAnalyses) {
                Map<String, Object> m = (Map<String, Object>) o;
                AIPropertyNote note = new AIPropertyNote();
                note.listingId = toIntegerOrNull(m.get("listingId"));
                note.analysisNote = (String) m.get("analysisNote");
                note.actionRecommendation = (String) m.get("actionRecommendation");
                result.propertyNotes.add(note);
            }
        }
        return result;
    }

    /** Fallback rule-based đơn giản khi AI lỗi/timeout — KHÔNG để cả API fail chỉ vì AI tạm không khả dụng. */
    private AIAnalysisResult buildFallbackAnalysis(List<PropertyFutureAnalysisDTO> results) {
        AIAnalysisResult r = new AIAnalysisResult();
        long totalDelta = results.stream().filter(x -> x.getMonthlyCashflowDelta() != null)
                .mapToLong(PropertyFutureAnalysisDTO::getMonthlyCashflowDelta).sum();
        r.overallScore = totalDelta >= 0 ? 70 : 40;
        r.overallNote = totalDelta >= 0
                ? "Dòng tiền thực tế tổng thể đang bằng hoặc tốt hơn kế hoạch ban đầu."
                : "Dòng tiền thực tế tổng thể đang thấp hơn kế hoạch ban đầu, cần rà soát lại.";
        r.overallRecommendation = totalDelta >= 0
                ? "Có thể tiếp tục duy trì danh mục hiện tại."
                : "Nên xem xét điều chỉnh giá thuê/chi phí vận hành cho các property có dòng tiền âm so với kế hoạch.";
        for (PropertyFutureAnalysisDTO x : results) {
            AIPropertyNote n = new AIPropertyNote();
            n.listingId = x.getListingId();
            boolean ok = x.getMonthlyCashflowDelta() == null || x.getMonthlyCashflowDelta() >= 0;
            n.analysisNote = ok ? "Dòng tiền thực tế đang bằng hoặc tốt hơn kế hoạch." : "Dòng tiền thực tế đang thấp hơn kế hoạch.";
            n.actionRecommendation = ok ? "Tiếp tục giữ." : "Xem xét điều chỉnh giá thuê hoặc chi phí vận hành.";
            r.propertyNotes.add(n);
        }
        return r;
    }

    // =====================================================================
    // HELPERS
    // =====================================================================

    private static String describeItem(GenerateFuturePlanRequest.SelectedPropertyItem item) {
        if (item.getListingId() != null) return "listingId=" + item.getListingId();
        if (item.getManualPropertyId() != null) return "manualPropertyId=" + item.getManualPropertyId();
        return "property không xác định";
    }

    private String resolvePropertyName(GenerateFuturePlanRequest.SelectedPropertyItem item, ProposedProperty planned) {
        if (planned != null && planned.getPropertyProjectName() != null) return planned.getPropertyProjectName();
        if (item.getListingId() != null) {
            Listing l = listingRepository.findById(item.getListingId()).orElse(null);
            if (l != null && l.getTitle() != null) return l.getTitle();
        }
        if (item.getManualPropertyId() != null) {
            Property p = propertyRepository.findById(item.getManualPropertyId()).orElse(null);
            if (p != null && p.getTitle() != null) return p.getTitle();
        }
        return "Bất động sản";
    }

    private static Long toLongOrNull(Object o) { return o instanceof Number ? ((Number) o).longValue() : null; }
    private static Integer toIntegerOrNull(Object o) { return o instanceof Number ? ((Number) o).intValue() : null; }
    private static Double toDoubleOrNull(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : null; }

    private Map<String, Object> toMap(PropertyFutureAnalysisDTO r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("listingId", r.getListingId());
        m.put("propertyProjectName", r.getPropertyProjectName());
        m.put("plannedMonthlyRentalCashflow", r.getPlannedMonthlyRentalCashflow());
        m.put("plannedNetCashflow", r.getPlannedNetCashflow());
        m.put("plannedRoiPercentage", r.getPlannedRoiPercentage());
        m.put("plannedEstimatedProfit", r.getPlannedEstimatedProfit());
        m.put("usagePurpose", r.getUsagePurpose());
        m.put("actualPurchasePrice", r.getActualPurchasePrice());
        m.put("actualMonthlyRevenue", r.getActualMonthlyRevenue());
        m.put("actualMonthlyOperatingCost", r.getActualMonthlyOperatingCost());
        m.put("actualMonthlyNetCashflow", r.getActualMonthlyNetCashflow());
        m.put("actualHoldingMonths", r.getActualHoldingMonths());
        m.put("actualAnnualCashflowYieldPercentage", r.getActualAnnualCashflowYieldPercentage());
        m.put("monthlyCashflowDelta", r.getMonthlyCashflowDelta());
        m.put("aiAnalysisNote", r.getAiAnalysisNote());
        m.put("aiActionRecommendation", r.getAiActionRecommendation());
        return m;
    }

    @SuppressWarnings("unchecked")
    private PropertyFutureAnalysisDTO mapToPropertyFutureAnalysisDTO(Map<String, Object> m) {
        return PropertyFutureAnalysisDTO.builder()
                .listingId(toIntegerOrNull(m.get("listingId")))
                .propertyProjectName((String) m.get("propertyProjectName"))
                .plannedMonthlyRentalCashflow(toLongOrNull(m.get("plannedMonthlyRentalCashflow")))
                .plannedNetCashflow(toLongOrNull(m.get("plannedNetCashflow")))
                .plannedRoiPercentage(toDoubleOrNull(m.get("plannedRoiPercentage")))
                .plannedEstimatedProfit(toLongOrNull(m.get("plannedEstimatedProfit")))
                .usagePurpose((String) m.get("usagePurpose"))
                .actualPurchasePrice(toLongOrNull(m.get("actualPurchasePrice")))
                .actualMonthlyRevenue(toLongOrNull(m.get("actualMonthlyRevenue")))
                .actualMonthlyOperatingCost(toLongOrNull(m.get("actualMonthlyOperatingCost")))
                .actualMonthlyNetCashflow(toLongOrNull(m.get("actualMonthlyNetCashflow")))
                .actualHoldingMonths(toIntegerOrNull(m.get("actualHoldingMonths")))
                .actualAnnualCashflowYieldPercentage(toDoubleOrNull(m.get("actualAnnualCashflowYieldPercentage")))
                .monthlyCashflowDelta(toLongOrNull(m.get("monthlyCashflowDelta")))
                .aiAnalysisNote((String) m.get("aiAnalysisNote"))
                .aiActionRecommendation((String) m.get("aiActionRecommendation"))
                .build();
    }
}
