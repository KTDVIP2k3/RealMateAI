package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestmentPlanServiceInterface;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CriteriaRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.SaveInvestmentPlanRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.*;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InvestmentPlanServiceImplement implements InvestmentPlanServiceInterface {

    @Autowired
    private Client geminiClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StrategyRepository strategyRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PropertyConditionRepository propertyConditionRepository;

    @Autowired
    private InvestmentProfileRepository investmentProfileRepository;

    @Autowired
    private InvestmentPortfolioRepository investmentPortfolioRepository;

    @Autowired
    private InvestmentCriteriaRepository investmentCriteriaRepository;

    @Autowired
    private PortfolioAllocationRepository portfolioAllocationRepository;

    @Autowired
    private PortfolioAllocationPropertyRepository portfolioAllocationPropertyRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private InvestmentScenarioRepository investmentScenarioRepository;

    @Autowired
    private ExecutionPlanRepository executionPlanRepository;

    @Autowired
    private AuthenUntil authenUntil;

    @Autowired
    private InvestorRepository investorRepository;

    @Autowired
    private InvestmentProfileVersionRepository investmentProfileVersionRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getListProfileByInvestor() {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null || currentAccount.getInvestor() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.fail("Unauthorized", "Không tìm thấy thông tin nhà đầu tư hiện tại."));
            }

            Investor investor = currentAccount.getInvestor();
            List<InvestmentProfile> profiles = investor.getInvestmentProfiles();
            if (profiles == null || profiles.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(Collections.emptyList(), "Danh sách profile trống."));
            }
            List<ProfileSimpleDTO> simpleProfiles = new ArrayList<>();
            for (InvestmentProfile profile : profiles) {
                if (profile.getProfileVersions() == null) {
                    continue;
                }


                InvestmentProfileVersion latestVersion = profile.getProfileVersions().stream()
                        .filter(v -> v.getCreatedAt() != null)
                        .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                        .max(Comparator.comparing(InvestmentProfileVersion::getCreatedAt))
                        .orElse(null);

                if (latestVersion == null) {
                    continue;
                }

                Long equity = (latestVersion != null) ? latestVersion.getEquity() : 0L;
                Long expectedRoi = (latestVersion != null) ? latestVersion.getExpectedRoi() : 0L;
                Long durationYear = (latestVersion != null) ? latestVersion.getDurationYear() : 0L;
                String conscious = (latestVersion != null) ? latestVersion.getConscious() : null;
                String ward = (latestVersion != null) ? latestVersion.getWard() : null;

                simpleProfiles.add(ProfileSimpleDTO.builder()
                        .investmentProfileId(profile.getInvestmentProfileId())
                        .name(profile.getName())
                        .conscious(conscious)
                        .ward(ward)
                        .isActive(latestVersion.getIsActive())
                        .equity(equity)
                        .expectedRoi(expectedRoi)
                        .durationYear(durationYear)
                        .strategyName(latestVersion.getStrategy() != null ? latestVersion.getStrategy().getName() : "N/A")
                        .createdAt(latestVersion.getCreatedAt())
                        .build());
            }

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(simpleProfiles, "Lấy danh sách kế hoạch đầu tư thành công"));

        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách profile của Investor", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getListViewsByProfileId(Integer profileId) {
        try {
            InvestmentProfile profile = investmentProfileRepository.findById(profileId).orElse(null);
            if (profile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Profile_Not_Found", "Không tìm thấy kế hoạch đầu tư với ID: " + profileId));
            }

            List<InvestmentProfileVersion> versions = profile.getProfileVersions();
            if (versions == null || versions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(Collections.emptyList(), "Kế hoạch này chưa có phiên bản lịch sử nào."));
            }

            List<ProfileVersionDTO> versionHistoryList = versions.stream()
                    .sorted(Comparator.comparing(InvestmentProfileVersion::getProfileVersionId).reversed())
                    .map(version -> {
                        String strategyName = (version.getStrategy() != null) ? version.getStrategy().getName() : "N/A";

                        return ProfileVersionDTO.builder()
                                .investmentProfileVersionId(version.getProfileVersionId())
                                .name(profile.getName() + " (Bản " + (version.getVersion() != null ? version.getVersion() : ("ID-" + version.getProfileVersionId())) + ")")
                                .conscious(version.getConscious())
                                .ward(version.getWard())
                                .isActive(version.getIsActive())
                                .equity(version.getEquity() != null ? version.getEquity() : 0L)
                                .expectedRoi(version.getExpectedRoi() != null ? version.getExpectedRoi() : 0L)
                                .durationYear(version.getDurationYear() != null ? version.getDurationYear() : 0L)
                                .strategyName(strategyName)
                                .createdAt(version.getCreatedAt())
                                .build();
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(versionHistoryList, "Lấy danh sách lịch sử phiên bản thành công"));

        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách lịch sử phiên bản của profile ID: {}", profileId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getProfileVersionDetailById(Integer profileVersionId) {
        try {
            InvestmentProfileVersion profileVersion = investmentProfileVersionRepository.findById(profileVersionId).orElse(null);
            if (profileVersion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Profile_Not_Found", "Investment profile version không tồn tại với ID: " + profileVersion));
            }

            InvestmentProfile profile = profileVersion.getInvestmentProfile();
            if (profile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Profile_Not_Found", "Không tìm thấy thông tin kế hoạch cha gốc của phiên bản này."));
            }


            List<InvestmentCriteriaDTO> criteriaDTOList = Collections.emptyList();
            if (profileVersion.getInvestmentCriterias() != null) {
                criteriaDTOList = profileVersion.getInvestmentCriterias().stream()
                        .map(criteria -> {
                            String typeName = (criteria.getPropertyType() != null)
                                    ? criteria.getPropertyType().getName() : null;
                            String conditionName = (criteria.getPropertyCondition() != null)
                                    ? criteria.getPropertyCondition().getName() : null;

                            return InvestmentCriteriaDTO.builder()
                                    .propertyTypeName(typeName)
                                    .propertyConditionName(conditionName)
                                    .build();
                        })
                        .collect(Collectors.toList());
            }


            List<String> legalStatusList = Collections.emptyList();
            if (profileVersion.getLegalStatus() != null && !profileVersion.getLegalStatus().isEmpty()) {
                try {
                    legalStatusList = objectMapper.readValue(profileVersion.getLegalStatus(), new TypeReference<List<String>>() {});
                } catch (Exception e) {
                    log.error("Lỗi khi giải mã chuỗi legalStatus JSON ở chi tiết", e);
                }
            }

            InvestmentProfileVersionDTO profileDTO = InvestmentProfileVersionDTO.builder()
                    .investmentProfileVersionId(profileVersion.getProfileVersionId())
                    .strategyName(profileVersion.getStrategy() != null ? profileVersion.getStrategy().getName() : null)
                    .name(profile.getName()) // Lấy tên từ profile cha
                    .equity(profileVersion.getEquity())
                    .loanCapital(profileVersion.getLoanCapital())
                    .reserveFund(profileVersion.getReserveFund())
                    .conscious(profileVersion.getConscious())
                    .ward(profileVersion.getWard())
                    .expectedRoi(profileVersion.getExpectedRoi())
                    .minProfit(profileVersion.getMinProfit())
                    .riskToleranceLevel(profileVersion.getRiskToleranceLevel())
                    .durationYear(profileVersion.getDurationYear())
                    .startDate(profileVersion.getStartDate())
                    .investmentType(profileVersion.getInvestmentType())
                    .investmentStrategyDetail(profileVersion.getInvestmentStrategyDetail())
                    .legalStatus(legalStatusList)
                    .version(null)
                    .isActive(profileVersion.getIsActive())
                    .createdAt(profileVersion.getCreatedAt())
                    .updatedAt(profileVersion.getUpdatedAt())
                    .investmentCriterias(criteriaDTOList)
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(profileDTO, "Lấy thông tin chi tiết profile version và tiêu chí thành công"));

        } catch (Exception e) {
            log.error("Lỗi khi lấy chi tiết profile ID: {}", profileVersionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getInvestmentPlanDetailByVersionId(Integer profileVersionId){
        try {
            InvestmentProfileVersion profileVersion = investmentProfileVersionRepository.findById(profileVersionId).orElse(null);
            if (profileVersion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Version_Not_Found", "Không tìm thấy phiên bản kế hoạch với ID: " + profileVersionId));
            }

            List<InvestmentScenarioDTO> scenarioDTOList = new ArrayList<>();
            if (profileVersion.getInvestmentScenarios() != null) {
                scenarioDTOList = profileVersion.getInvestmentScenarios().stream()
                        .map(scenario -> {
                            int totalMonths = 0;
                            if (profileVersion.getDurationYear() != null) {
                                totalMonths = (int) (profileVersion.getDurationYear() * 12);
                            }
                            return InvestmentScenarioDTO.builder()
                                    .pkInvestmentScenarioId(scenario.getInvestmentScenarioId())
                                    .enumScenarioType(scenario.getScenarioType())
                                    .decimprofitYield(scenario.getExpectedReturnRate())
                                    .textMarketNote(scenario.getDescription())
                                    .decimmonthlyCashflow(0.0)
                                    .decimprobability(0.0)
                                    .durationMonths(totalMonths)
                                    .decimpriceGrowthMin(0.0)
                                    .decimpriceGrowthMax(0.0)
                                    .build();
                        })
                        .collect(Collectors.toList());
            }


            ExecutionPlanDTO executionPlanDTO = null;
            if (profileVersion.getExecutionPlans() != null && !profileVersion.getExecutionPlans().isEmpty()) {
                ExecutionPlan activePlan = profileVersion.getExecutionPlans().get(0);
                if (activePlan.getDescription() != null && !activePlan.getDescription().isEmpty()) {
                    try {
                        executionPlanDTO = objectMapper.readValue(activePlan.getDescription(), ExecutionPlanDTO.class);
                    } catch (Exception jsonEx) {
                        log.error("Error parsing ExecutionPlan JSON for version ID: {}", profileVersionId, jsonEx);
                    }
                }
            }

            List<InvestmentPortfolioDTO> portfolioDTOList = new ArrayList<>();
            if (profileVersion.getInvestmentPortfolios() != null) {
                int portfolioSeq = 1;
                for (InvestmentPortfolio ip : profileVersion.getInvestmentPortfolios()) {

                    List<PortfolioAllocationDTO> allocationDTOList = new ArrayList<>();

                    List<PortfolioAllocation> allocationsInDb = portfolioAllocationRepository.findAll().stream()
                            .filter(pa -> pa.getInvestmentPortfolio() != null &&
                                    pa.getInvestmentPortfolio().getInvestmentPortfolioId().equals(ip.getInvestmentPortfolioId()))
                            .collect(Collectors.toList());

                    for (PortfolioAllocation pa : allocationsInDb) {
                        List<PortfolioAllocationPropertyDTO> propertyDTOList = new ArrayList<>();

                        List<PortfolioAllocationProperty> propertiesInDb = portfolioAllocationPropertyRepository.findAll().stream()
                                .filter(pap -> pap.getPortfolioAllocation() != null &&
                                        pap.getPortfolioAllocation().getPortfolioAllocationId().equals(pa.getPortfolioAllocationId()))
                                .collect(Collectors.toList());

                        for (PortfolioAllocationProperty pap : propertiesInDb) {
                            Property prop = pap.getProperty();

                            Integer listingId = null;
                            String title = null;
                            int area = 0;
                            double price = 0.0;
                            String description = null;

                            if (prop != null) {
                                title = prop.getTitle();
                                area = prop.getArea() != null ? prop.getArea().intValue() : 0;

                                if (prop.getListings() != null && !prop.getListings().isEmpty()) {
                                    Listing activeListing = prop.getListings().get(0);
                                    listingId = activeListing.getListingId();
                                    price = activeListing.getPrice() != null ? activeListing.getPrice().doubleValue() : 0.0;

                                    String conditionName = (prop.getPropertyCondition() != null)
                                            ? prop.getPropertyCondition().getName() : "Standard Condition";
                                    description = "[" + conditionName + "] - " + (activeListing.getDescription() != null ? activeListing.getDescription() : "");
                                }
                            }

                            propertyDTOList.add(PortfolioAllocationPropertyDTO.builder()
                                    .portfolioAllocationPropertyId(listingId)
                                    .propertyProjectName(title)
                                    .area(area)
                                    .valuePrice(price)
                                    .description(description)
                                    .build());
                        }

                        String propertyTypeName = "Real Estate";
                        if (pa.getPortfolio() != null) {
                            propertyTypeName = pa.getPortfolio().getName();
                        }

                        allocationDTOList.add(PortfolioAllocationDTO.builder()
                                .propertyTypeName(propertyTypeName)
                                .properties(propertyDTOList)
                                .build());
                    }

                    portfolioDTOList.add(InvestmentPortfolioDTO.builder()
                            .investmentPortfolioId(portfolioSeq++)
                            .portfolioId(ip.getPortfolio() != null ? ip.getPortfolio().getPortfolioId() : null)
                            .portfolioName(ip.getPortfolio() != null ? ip.getPortfolio().getName() : "Investment Portfolio")
                            .percentage(ip.getPercentage())
                            .capital(ip.getCapital())
                            .allocations(allocationDTOList)
                            .build());
                }
            }

            InvestmentPlanDTO finalOutput = InvestmentPlanDTO.builder()
                    .scenarios(scenarioDTOList)
                    .executionPlan(executionPlanDTO)
                    .investmentPortfolios(portfolioDTOList)
                    .build();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(finalOutput, "Lấy chi tiết kế hoạch đầu tư thành công"));

        } catch (Exception e) {
            log.error("Error in getInvestmentPlanDetailByProfileVersionId for profile version ID: {}", profileVersionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> generateCompleteInvestmentPlan(InvestmentPlanRequest request) {
        try {
            Strategy strategy = strategyRepository.findById(request.getStrategy_id()).orElse(null);
            if (strategy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Strategy_Not_Found", "Investment strategy not found."));
            }

            Account account = authenUntil.getCurrentUSer();
            if(account.getInvestor() == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(null, "Investor survey does not exist. Please create to use this function"));
            }

            final String inputName = request.getName() != null ? request.getName().trim() : "";
            if (!inputName.isEmpty()) {
                final Integer currentInvestorId = account.getInvestor().getInvestorId();

                boolean isNameExists = investmentProfileRepository.findAll().stream()
                        .anyMatch(p -> p.getInvestor() != null
                                && p.getInvestor().getInvestorId().equals(currentInvestorId)
                                && p.getName() != null
                                && p.getName().equalsIgnoreCase(inputName));

                if (isNameExists) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(ApiResponse.fail("Name_Already_Exists", "The investment plan name '" + inputName + "' already exists. Please choose another name."));
                }
            }

            List<InvestmentPortfolioDTO> portfolios = processStage1Portfolios(request, strategy);
            processStage2EnrichProperties(request, portfolios);
            InvestmentPlanDTO finalOutput = processStage3ScenariosAndExecution(request, portfolios);

            saveInvestmentPlanToDatabase(request, finalOutput, strategy);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(finalOutput, "Generate and save complete investment plan successfully"));
        } catch (Exception e) {
            log.error("Error in generateCompleteInvestmentPlan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> savePlanToDatabaseDirectly(SaveInvestmentPlanRequest saveRequest) {
        try {
            if (saveRequest == null || saveRequest.getInputRequest() == null || saveRequest.getAiOutputData() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Invalid_Payload", "Invalid save request data payload."));
            }

            Strategy strategy = strategyRepository.findById(saveRequest.getInputRequest().getStrategy_id()).orElse(null);
            if (strategy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Strategy_Not_Found", "Investment strategy not found."));
            }

            saveInvestmentPlanToDatabase(saveRequest.getInputRequest(), saveRequest.getAiOutputData(), strategy);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(null, "Investment plan logged and saved successfully"));
        } catch (Exception e) {
            log.error("Error in savePlanToDatabaseDirectly", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private List<InvestmentPortfolioDTO> processStage1Portfolios(InvestmentPlanRequest request, Strategy strategy) {
        List<StrategyPortfolio> activeRelations = strategy.getStrategyPortfolios().stream()
                .filter(sp -> sp.getIsActive() != null && sp.getIsActive())
                .collect(Collectors.toList());

        if (activeRelations.isEmpty()) {
            throw new RuntimeException("No active portfolios linked to this strategy.");
        }

        List<Map<String, Object>> portfolioInputList = new ArrayList<>(activeRelations.size());
        for (StrategyPortfolio relation : activeRelations) {
            Portfolio p = relation.getPortfolio();
            Map<String, Object> map = new HashMap<>();
            map.put("portfolioId", p.getPortfolioId());
            map.put("portfolioName", p.getName());
            portfolioInputList.add(map);
        }

        String promptStage1 = String.format(
                "Based on the client's capital:\n- Equity: %d\n- Loan Capital: %d\n- Reserve Fund: %d\n" +
                        "Allocate capital into the following target portfolios (Total percentage must equal 100%%): %s",
                request.getEquity(), request.getLoanCapital(), request.getReserveFund(), portfolioInputList.toString()
        );

        GenerateContentConfig configStage1 = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .responseSchema(Schema.builder()
                        .type("ARRAY")
                        .items(Schema.builder()
                                .type("OBJECT")
                                .properties(Map.of(
                                        "portfolioId", Schema.builder().type("INTEGER").build(),
                                        "percentage", Schema.builder().type("INTEGER").build(),
                                        "capital", Schema.builder().type("NUMBER").build()
                                ))
                                .required(List.of("portfolioId", "percentage", "capital"))
                                .build())
                        .build())
                .build();

        int retryCount = 0;
        GenerateContentResponse response = null;

        while (retryCount < 5) {
            try {
                response = geminiClient.models.generateContent("gemini-2.5-flash", promptStage1, configStage1);
                break;
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                // Bổ sung bắt cả lỗi 429 quá tải lượt gọi lẫn lỗi 503 từ server Google
                if (errorMsg.contains("429") || errorMsg.contains("503") || errorMsg.contains("Unavailable") || errorMsg.contains("Quota exceeded") || errorMsg.contains("rate-limits")) {
                    retryCount++;
                    try {
                        // Tăng thời gian ngủ lên 8 giây để chắc chắn reset cửa sổ RPM
                        Thread.sleep(8000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                } else {
                    throw e;
                }
            }
        }

        if (response == null) {
            throw new RuntimeException("Gemini API đang bận hoặc quá hạn mức (429/503). Vui lòng thử lại sau vài giây.");
        }

        try {
            List<Map<String, Object>> parsedAllocations = objectMapper.readValue(response.text().trim(), new TypeReference<List<Map<String, Object>>>() {});
            List<InvestmentPortfolioDTO> investmentPortfolios = new ArrayList<>(parsedAllocations.size());
            int seq = 1;

            Map<Integer, String> portfolioNameMap = activeRelations.stream()
                    .collect(Collectors.toMap(r -> r.getPortfolio().getPortfolioId(), r -> r.getPortfolio().getName(), (v1, v2) -> v1));

            for (Map<String, Object> item : parsedAllocations) {
                Integer pId = (Integer) item.get("portfolioId");
                Integer pPercentage = (Integer) item.get("percentage");
                Double pCapital = ((Number) item.get("capital")).doubleValue();
                String pName = portfolioNameMap.getOrDefault(pId, "Investment Portfolio");

                investmentPortfolios.add(InvestmentPortfolioDTO.builder()
                        .investmentPortfolioId(seq++)
                        .portfolioId(pId)
                        .portfolioName(pName)
                        .percentage(pPercentage)
                        .capital(pCapital)
                        .allocations(new ArrayList<>())
                        .build());
            }
            return investmentPortfolios;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void processStage2EnrichProperties(InvestmentPlanRequest request, List<InvestmentPortfolioDTO> portfolios) {
        List<CriteriaRequest> criteriaList = request.getCriteriaList();
        if (criteriaList == null || criteriaList.isEmpty()) {
            return;
        }

        List<Integer> typeIds = criteriaList.stream()
                .map(CriteriaRequest::getPropertyTypeId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Integer, String> propertyTypeCache = propertyTypeRepository.findAllById(typeIds).stream()
                .collect(Collectors.toMap(PropertyType::getPropertyTypeId, PropertyType::getName, (v1, v2) -> v1));

        int totalTypes = criteriaList.size();
        String ward = request.getWard();

        for (InvestmentPortfolioDTO portfolio : portfolios) {
            double capitalPerType = portfolio.getCapital() / totalTypes;
            List<PortfolioAllocationDTO> allocations = new ArrayList<>(totalTypes);

            for (CriteriaRequest crit : criteriaList) {
                Integer typeId = crit.getPropertyTypeId();
                String currentPropertyTypeName = propertyTypeCache.getOrDefault(typeId, "Real Estate");

                PortfolioAllocationDTO allocationDTO = PortfolioAllocationDTO.builder()
                        .propertyTypeName(currentPropertyTypeName)
                        .properties(new ArrayList<>())
                        .build();

                List<PortfolioAllocationPropertyDTO> dbRealProperties = queryRealWarehouseProperties(
                        capitalPerType,
                        ward,
                        typeId
                );

                allocationDTO.setProperties(dbRealProperties);
                allocations.add(allocationDTO);
            }
            portfolio.setAllocations(allocations);
        }
    }

    private List<PortfolioAllocationPropertyDTO> queryRealWarehouseProperties(Double capitalPerType, String ward, Integer propertyTypeId) {
        Long maxPriceAllowed = capitalPerType.longValue();

        List<Listing> matchedListings = listingRepository.findRealPropertiesByAiStrategy(
                ward, propertyTypeId, maxPriceAllowed
        );

        if (matchedListings.isEmpty()) {
//            return Collections.singletonList(PortfolioAllocationPropertyDTO.builder()
//                    .portfolioAllocationPropertyId(null)
//                    .propertyProjectName(null)
//                    .area(0)
//                    .valuePrice(0.0)
//                    .description(null)
//                    .build());

            return Collections.singletonList(PortfolioAllocationPropertyDTO.builder()
                    .portfolioAllocationPropertyId(null)
                    .propertyProjectName(null)
                    .area(null)
                    .valuePrice(null)
                    .description(null)
                    .build());
        }

        int limit = Math.min(matchedListings.size(), 2);
        List<PortfolioAllocationPropertyDTO> resultList = new ArrayList<>(limit);

        for (int i = 0; i < limit; i++) {
            Listing listing = matchedListings.get(i);
            Property property = listing.getProperty();

            String conditionName = (property != null && property.getPropertyCondition() != null)
                    ? property.getPropertyCondition().getName()
                    : "Standard Condition";

            String listingDesc = listing.getDescription() != null ? listing.getDescription() : "";
            String finalDescription = "[" + conditionName + "] - " + listingDesc;

            resultList.add(PortfolioAllocationPropertyDTO.builder()
                    .portfolioAllocationPropertyId(listing.getListingId())
                    .propertyProjectName(property != null ? property.getTitle() : null)
                    .area((property != null && property.getArea() != null) ? property.getArea().intValue() : 0)
                    .valuePrice(listing.getPrice() != null ? listing.getPrice().doubleValue() : 0.0)
                    .description(finalDescription)
                    .build());
        }

        return resultList;
    }

    private InvestmentPlanDTO processStage3ScenariosAndExecution(InvestmentPlanRequest request, List<InvestmentPortfolioDTO> portfolios) {
        String cleanTreeJson = "";
        try {
            cleanTreeJson = objectMapper.writeValueAsString(portfolios);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        String promptStage3 = "Analyze the portfolio structure and 3-tier allocation model to return a comprehensive market scenario report in JSON format: " + cleanTreeJson;

        GenerateContentConfig configStage3 = GenerateContentConfig.builder()
                .responseMimeType("application/json")
                .responseSchema(Schema.builder()
                        .type("OBJECT")
                        .properties(Map.of(
                                "score", Schema.builder().type("INTEGER").build(),
                                "scenarios", Schema.builder()
                                        .type("ARRAY")
                                        .items(Schema.builder()
                                                .type("OBJECT")
                                                .properties(Map.of(
                                                        "pkInvestmentScenarioId", Schema.builder().type("INTEGER").build(),
                                                        "enumScenarioType", Schema.builder().type("STRING").build(),
                                                        "decimprofitYield", Schema.builder().type("NUMBER").build(),
                                                        "decimmonthlyCashflow", Schema.builder().type("NUMBER").build(),
                                                        "decimprobability", Schema.builder().type("NUMBER").build(),
                                                        "textMarketNote", Schema.builder().type("STRING").build(),
                                                        "durationMonths", Schema.builder().type("INTEGER").build(),
                                                        "decimpriceGrowthMin", Schema.builder().type("NUMBER").build(),
                                                        "decimpriceGrowthMax", Schema.builder().type("NUMBER").build()
                                                )).build())
                                        .build(),
                                "executionPlan", Schema.builder()
                                        .type("OBJECT")
                                        .properties(Map.ofEntries(
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
        GenerateContentResponse responseStage3 = null;

        while (retryCount < 5) {
            try {
                responseStage3 = geminiClient.models.generateContent("gemini-2.5-flash", promptStage3, configStage3);
                break;
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
                // Nhận diện lỗi nghẽn mạch API hoặc lỗi dịch vụ gián đoạn từ phía đối tác để kích hoạt chờ đợi
                if (errorMsg.contains("429") || errorMsg.contains("503") || errorMsg.contains("Unavailable") || errorMsg.contains("Quota exceeded") || errorMsg.contains("rate-limits")) {
                    retryCount++;
                    try {
                        Thread.sleep(8000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ie);
                    }
                } else {
                    throw e;
                }
            }
        }

        if (responseStage3 == null) {
            throw new RuntimeException("Gemini API đang bận hoặc quá hạn mức (429/503). Vui lòng thử lại sau vài giây.");
        }

        try {
            InvestmentPlanDTO finalInvestmentPlan = objectMapper.readValue(responseStage3.text().trim(), InvestmentPlanDTO.class);
            finalInvestmentPlan.setInvestmentPortfolios(portfolios);
            return finalInvestmentPlan;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }



    private void saveInvestmentPlanToDatabase(InvestmentPlanRequest request, InvestmentPlanDTO output, Strategy strategy) throws Exception {
        LocalDateTime now = LocalDateTime.now();

        Investor dbInvestor = null;
        Account currentAccount = authenUntil.getCurrentUSer();
        if (currentAccount != null && currentAccount.getInvestor() != null) {
            dbInvestor = investorRepository.findById(currentAccount.getInvestor().getInvestorId()).orElse(null);
        }

        if (request.getStrategy_id() == null) {
            throw new IllegalArgumentException("Strategy ID must not be null");
        }

        Map<String, Object> strategyDetailMap = request.getInvestmentStrategyDetail();

        if (strategyDetailMap == null) {
            strategyDetailMap = new HashMap<>();
        }

        String legalStatusJson = null;
        if (request.getLegalStatus() != null && !request.getLegalStatus().isEmpty()) {
            legalStatusJson = objectMapper.writeValueAsString(request.getLegalStatus());
        }


        InvestmentProfile profile = InvestmentProfile.builder()
                .investor(dbInvestor)
                .name(request.getName() != null ? request.getName() : "AI Investment Plan - " + now)
                .createdAt(now)
                .isActive(true)
                .updatedAt(now)
                .profileVersions(new ArrayList<>())
                .build();

        InvestmentProfile savedProfile = investmentProfileRepository.save(profile);

        InvestmentProfileVersion versionEntity = InvestmentProfileVersion.builder()
                .investmentProfile(savedProfile)
                .strategy(strategy)
                .equity(request.getEquity())
                .loanCapital(request.getLoanCapital())
                .reserveFund(request.getReserveFund())
                .conscious(request.getConscious())
                .ward(request.getWard())
                .expectedRoi(request.getExpectedRoi())
                .minProfit(request.getMinProfit())
                .riskToleranceLevel(request.getRiskToleranceLevel())
                .durationYear(request.getDurationYear())
                .startDate(request.getStartDate())
                .investmentType(request.getInvestmentType())
                .investmentStrategyDetail(strategyDetailMap)
                .legalStatus(legalStatusJson)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .investmentCriterias(new ArrayList<>())
                .investmentScenarios(new ArrayList<>())
                .investmentPortfolios(new ArrayList<>())
                .executionPlans(new ArrayList<>())
                .build();


        InvestmentProfileVersion savedVersion = investmentProfileVersionRepository.save(versionEntity);

        if (request.getCriteriaList() != null && !request.getCriteriaList().isEmpty()) {
            for (CriteriaRequest critRequest : request.getCriteriaList()) {
                if (critRequest.getPropertyTypeId() != null || critRequest.getPropertyConditionId() != null) {
                    PropertyType pType = null;
                    if (critRequest.getPropertyTypeId() != null) {
                        pType = propertyTypeRepository.findById(critRequest.getPropertyTypeId()).orElse(null);
                    }

                    PropertyCondition pCondition = null;
                    if (critRequest.getPropertyConditionId() != null) {
                        pCondition = propertyConditionRepository.findById(critRequest.getPropertyConditionId()).orElse(null);
                    }

                    InvestmentCriteria criteriaEntity = InvestmentCriteria.builder()
                            .investmentProfileVersion(savedVersion)
                            .propertyType(pType)
                            .propertyCondition(pCondition)
                            .build();
                    investmentCriteriaRepository.save(criteriaEntity);
                }
            }
        }
        if (output != null && output.getScenarios() != null) {
            for (var scenarioDTO : output.getScenarios()) {
                InvestmentScenario scenarioEntity = InvestmentScenario.builder()
                        .investmentProfileVersion(savedVersion)
                        .name(scenarioDTO.getEnumScenarioType())
                        .scenarioType(scenarioDTO.getEnumScenarioType())
                        .expectedReturnRate(scenarioDTO.getDecimprofitYield())
                        .description(scenarioDTO.getTextMarketNote())
                        .isActive(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                investmentScenarioRepository.save(scenarioEntity);
            }
        }

        if (output != null && output.getExecutionPlan() != null) {
            var planDTO = output.getExecutionPlan();
            String descJson = objectMapper.writeValueAsString(planDTO);

            ExecutionPlan planEntity = ExecutionPlan.builder()
                    .investmentProfileVersion(savedVersion)
                    .name("AI Execution Plan Details")
                    .description(descJson)
                    .status("ACTIVE")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            executionPlanRepository.save(planEntity);
        }

        if (output != null && output.getInvestmentPortfolios() != null) {
            List<Portfolio> allowedPortfolios = new ArrayList<>();
            if (strategy.getStrategyPortfolios() != null) {
                for (StrategyPortfolio sp : strategy.getStrategyPortfolios()) {
                    if (sp.getPortfolio() != null) {
                        allowedPortfolios.add(sp.getPortfolio());
                    }
                }
            }

            if (allowedPortfolios.isEmpty()) {
                allowedPortfolios = portfolioRepository.findAll();
            }

            for (var portDTO : output.getInvestmentPortfolios()) {
                if (portDTO.getPortfolioName() == null) {
                    continue;
                }

                final String targetName = portDTO.getPortfolioName().trim();
                Portfolio dbPortfolio = allowedPortfolios.stream()
                        .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(targetName))
                        .findFirst()
                        .orElse(null);

                if (dbPortfolio == null) {
                    continue;
                }

                InvestmentPortfolio portEntity = InvestmentPortfolio.builder()
                        .investmentProfileVersion(savedVersion)
                        .portfolio(dbPortfolio)
                        .percentage(portDTO.getPercentage())
                        .capital(portDTO.getCapital())
                        .isActive(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                InvestmentPortfolio savedPortEntity = investmentPortfolioRepository.save(portEntity);

                if (portDTO.getAllocations() != null) {
                    for (var allocDTO : portDTO.getAllocations()) {
                        PortfolioAllocation allocEntity = PortfolioAllocation.builder()
                                .portfolio(dbPortfolio)
                                .investmentPortfolio(savedPortEntity)
                                .isActive(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();

                        PortfolioAllocation savedAllocEntity = portfolioAllocationRepository.save(allocEntity);

                        if (allocDTO.getProperties() != null) {
                            for (var propDTO : allocDTO.getProperties()) {
                                if (propDTO.getPortfolioAllocationPropertyId() != null) {
                                    Listing listing = listingRepository.findById(propDTO.getPortfolioAllocationPropertyId()).orElse(null);
                                    Property propertyRelation = (listing != null) ? listing.getProperty() : null;

                                    PortfolioAllocationProperty propEntity = PortfolioAllocationProperty.builder()
                                            .portfolioAllocation(savedAllocEntity)
                                            .property(propertyRelation)
                                            .weight(1.0)
                                            .isActive(true)
                                            .createdAt(now)
                                            .updatedAt(now)
                                            .build();
                                    portfolioAllocationPropertyRepository.save(propEntity);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateExistingInvestmentPlan(Integer currentProfileId, UpdateInvestmentPlanRequest request) {
        try {
            InvestmentProfile oldProfile = investmentProfileRepository.findById(currentProfileId).orElse(null);
            if (oldProfile == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Profile_Not_Found", "The investment profile you want to update does not exist."));
            }

            Strategy strategy = strategyRepository.findById(request.getStrategy_id()).orElse(null);
            if (strategy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Strategy_Not_Found", "Investment strategy not found."));
            }

            InvestmentPlanRequest internalRequest = new InvestmentPlanRequest();
            internalRequest.setStrategy_id(request.getStrategy_id());
            internalRequest.setName(oldProfile.getName());
            internalRequest.setEquity(request.getEquity());
            internalRequest.setLoanCapital(request.getLoanCapital());
            internalRequest.setReserveFund(request.getReserveFund());
            internalRequest.setConscious(request.getConscious());
            internalRequest.setWard(request.getWard());
            internalRequest.setExpectedRoi(request.getExpectedRoi());
            internalRequest.setMinProfit(request.getMinProfit());
            internalRequest.setRiskToleranceLevel(request.getRiskToleranceLevel());
            internalRequest.setDurationYear(request.getDurationYear());
            internalRequest.setStartDate(request.getStartDate());
            internalRequest.setInvestmentType(request.getInvestmentType());
            internalRequest.setInvestmentStrategyDetail(request.getInvestmentStrategyDetail());
            internalRequest.setLegalStatus(request.getLegalStatus());
            internalRequest.setCriteriaList(request.getCriteriaList());

            List<InvestmentPortfolioDTO> portfolios = processStage1Portfolios(internalRequest, strategy);
            processStage2EnrichProperties(internalRequest, portfolios);
            InvestmentPlanDTO finalOutput = processStage3ScenariosAndExecution(internalRequest, portfolios);

            saveUpdatePlanToDatabase(oldProfile, internalRequest, finalOutput, strategy);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(finalOutput, "Updated and saved new version of investment plan successfully"));
        } catch (Exception e) {
            log.error("Error in updateExistingInvestmentPlan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private void saveUpdatePlanToDatabase(InvestmentProfile oldProfile, InvestmentPlanRequest request, InvestmentPlanDTO output, Strategy strategy) throws Exception {
        LocalDateTime now = LocalDateTime.now();


        oldProfile.setUpdatedAt(now);
        InvestmentProfile savedProfile = investmentProfileRepository.save(oldProfile);

        final Integer currentInvestorId = oldProfile.getInvestor() != null ? oldProfile.getInvestor().getInvestorId() : null;
        final String targetName = oldProfile.getName();


        String legalStatusJson = null;
        if (request.getLegalStatus() != null && !request.getLegalStatus().isEmpty()) {
            legalStatusJson = objectMapper.writeValueAsString(request.getLegalStatus());
        }

        Map<String, Object> strategyDetailMap = request.getInvestmentStrategyDetail();
        if (strategyDetailMap == null) {
            strategyDetailMap = new HashMap<>();
        }

        InvestmentProfileVersion versionEntity = InvestmentProfileVersion.builder()
                .investmentProfile(savedProfile)
                .strategy(strategy)
                .equity(request.getEquity())
                .loanCapital(request.getLoanCapital())
                .reserveFund(request.getReserveFund())
                .conscious(request.getConscious())
                .ward(request.getWard())
                .expectedRoi(request.getExpectedRoi())
                .minProfit(request.getMinProfit())
                .riskToleranceLevel(request.getRiskToleranceLevel())
                .durationYear(request.getDurationYear())
                .startDate(request.getStartDate())
                .legalStatus(legalStatusJson)
                .investmentType(request.getInvestmentType())
                .investmentStrategyDetail(strategyDetailMap)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .investmentCriterias(new ArrayList<>())
                .investmentScenarios(new ArrayList<>())
                .investmentPortfolios(new ArrayList<>())
                .executionPlans(new ArrayList<>())
                .build();

        InvestmentProfileVersion savedVersion = investmentProfileVersionRepository.save(versionEntity);
        if (request.getCriteriaList() != null && !request.getCriteriaList().isEmpty()) {
            for (CriteriaRequest critRequest : request.getCriteriaList()) {
                if (critRequest.getPropertyTypeId() != null || critRequest.getPropertyConditionId() != null) {
                    PropertyType pType = null;
                    if (critRequest.getPropertyTypeId() != null) {
                        pType = propertyTypeRepository.findById(critRequest.getPropertyTypeId()).orElse(null);
                    }

                    PropertyCondition pCondition = null;
                    if (critRequest.getPropertyConditionId() != null) {
                        pCondition = propertyConditionRepository.findById(critRequest.getPropertyConditionId()).orElse(null);
                    }

                    InvestmentCriteria criteriaEntity = InvestmentCriteria.builder()
                            .investmentProfileVersion(savedVersion)
                            .propertyType(pType)
                            .propertyCondition(pCondition)
                            .build();
                    investmentCriteriaRepository.save(criteriaEntity);
                }
            }
        }

        if (output != null && output.getScenarios() != null) {
            for (var scenarioDTO : output.getScenarios()) {
                InvestmentScenario scenarioEntity = InvestmentScenario.builder()
                        .investmentProfileVersion(savedVersion)
                        .name(scenarioDTO.getEnumScenarioType())
                        .scenarioType(scenarioDTO.getEnumScenarioType())
                        .expectedReturnRate(scenarioDTO.getDecimprofitYield())
                        .description(scenarioDTO.getTextMarketNote())
                        .isActive(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                investmentScenarioRepository.save(scenarioEntity);
            }
        }

        if (output != null && output.getExecutionPlan() != null) {
            var planDTO = output.getExecutionPlan();
            String descJson = objectMapper.writeValueAsString(planDTO);

            ExecutionPlan planEntity = ExecutionPlan.builder()
                    .investmentProfileVersion(savedVersion)
                    .name("AI Execution Plan Details")
                    .description(descJson)
                    .status("ACTIVE")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            executionPlanRepository.save(planEntity);
        }

        if (output != null && output.getInvestmentPortfolios() != null) {
            List<Portfolio> allowedPortfolios = new ArrayList<>();
            if (strategy.getStrategyPortfolios() != null) {
                for (StrategyPortfolio sp : strategy.getStrategyPortfolios()) {
                    if (sp.getPortfolio() != null) {
                        allowedPortfolios.add(sp.getPortfolio());
                    }
                }
            }

            if (allowedPortfolios.isEmpty()) {
                allowedPortfolios = portfolioRepository.findAll();
            }

            for (var portDTO : output.getInvestmentPortfolios()) {
                if (portDTO.getPortfolioName() == null) {
                    continue;
                }

                final String targetNamePort = portDTO.getPortfolioName().trim();
                Portfolio dbPortfolio = allowedPortfolios.stream()
                        .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(targetNamePort))
                        .findFirst()
                        .orElse(null);

                if (dbPortfolio == null) {
                    continue;
                }

                InvestmentPortfolio portEntity = InvestmentPortfolio.builder()
                        .investmentProfileVersion(savedVersion)
                        .portfolio(dbPortfolio)
                        .percentage(portDTO.getPercentage())
                        .capital(portDTO.getCapital())
                        .isActive(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();

                InvestmentPortfolio savedPortEntity = investmentPortfolioRepository.save(portEntity);

                if (portDTO.getAllocations() != null) {
                    for (var allocDTO : portDTO.getAllocations()) {
                        PortfolioAllocation allocEntity = PortfolioAllocation.builder()
                                .portfolio(dbPortfolio)
                                .investmentPortfolio(savedPortEntity)
                                .isActive(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build();

                        PortfolioAllocation savedAllocEntity = portfolioAllocationRepository.save(allocEntity);

                        if (allocDTO.getProperties() != null) {
                            for (var propDTO : allocDTO.getProperties()) {
                                if (propDTO.getPortfolioAllocationPropertyId() != null) {
                                    Listing listing = listingRepository.findById(propDTO.getPortfolioAllocationPropertyId()).orElse(null);
                                    Property propertyRelation = (listing != null) ? listing.getProperty() : null;

                                    PortfolioAllocationProperty propEntity = PortfolioAllocationProperty.builder()
                                            .portfolioAllocation(savedAllocEntity)
                                            .property(propertyRelation)
                                            .weight(1.0)
                                            .isActive(true)
                                            .createdAt(now)
                                            .updatedAt(now)
                                            .build();
                                    portfolioAllocationPropertyRepository.save(propEntity);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> deleteInvestmentPlanVersion(Integer versionId) {
        try {
            InvestmentProfileVersion version = investmentProfileVersionRepository.findById(versionId).orElse(null);
            if (version == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Version_Not_Found", "Không tìm thấy phiên bản kế hoạch với ID: " + versionId));
            }

            if (Boolean.FALSE.equals(version.getIsActive())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Already_Deleted", "Phiên bản này đã được xóa từ trước."));
            }

            version.setIsActive(false);
            version.setUpdatedAt(LocalDateTime.now());
            investmentProfileVersionRepository.save(version);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Xóa phiên bản kế hoạch đầu tư thành công"));

        } catch (Exception e) {
            log.error("Lỗi khi xóa phiên bản với versionId: {}", versionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", "Đã xảy ra lỗi: " + e.getMessage()));
        }
    }

}