package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestmentPlanServiceInterface;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.GSU26SE22_SU26SE002.RealMateAI.model.*;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.*;
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
    private MembershipSubscriptionRepository membershipSubscriptionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StrategyRepository strategyRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private ProposedPropertyRepository proposedPropertyRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private PropertyConditionRepository propertyConditionRepository;

    @Autowired
    private InvestmentProfileRepository investmentProfileRepository;

    @Autowired
    private InvestmentCriteriaRepository investmentCriteriaRepository;


    @Autowired
    private InvestmentScenarioRepository investmentScenarioRepository;


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
                Long totalCapital = (latestVersion != null) ? latestVersion.getTotalCapital() : 0l;
//                Long expectedRoi = (latestVersion != null) ? latestVersion.getExpectedRoi() : 0L;
//                Long durationYear = (latestVersion != null) ? latestVersion.getDurationYear() : 0L;
                String conscious = (latestVersion != null) ? latestVersion.getConscious() : null;

                List<String> wards = latestVersion != null && latestVersion.getWards() != null
                        ? latestVersion.getWards()
                        : new ArrayList<>();

//                List<InvestmentPortfolioRequest> investmentPortfolioRequests = latestVersion.getInvestmentPortfolios().stream()
//                        .map(investmentPortfolio -> new InvestmentPortfolioRequest(
//                                investmentPortfolio.getPortfolio().getName(),
//                                investmentPortfolio.getPercentage()
//                        ))
//                                .collect(Collectors.toList());

//                Integer matchScore = latestVersion.getMatch_score() != 0 ? latestVersion.getMatch_score() : 0;
//                if (latestVersion.getExecutionPlans() != null && !latestVersion.getExecutionPlans().isEmpty()) {
//                    matchScore = latestVersion.getExecutionPlans().get(0).getMatch_score();
//                    if (matchScore == null) matchScore = 0;
//                }

                simpleProfiles.add(ProfileSimpleDTO.builder()
                        .investmentProfileId(profile.getInvestmentProfileId())
                        .latestVersionId(latestVersion.getProfileVersionId())
                        .totalCapital(latestVersion.getTotalCapital() != 0 ? latestVersion.getTotalCapital() : 0)
//                        .matchScore(matchScore)
                        .name(profile.getName())
                        .consciousName(conscious)
                        .wardName(wards)
                        .isActive(latestVersion.getIsActive())
                        .equity(equity)
                        .strategyName(latestVersion.getStrategy() != null ? latestVersion.getStrategy().getName() : "N/A")
//                        .investmentPortfolioRequests(investmentPortfolioRequests)
                        .createdAt(latestVersion.getCreatedAt())
                        .build());
            }

            simpleProfiles.sort(Comparator.comparing(
                    ProfileSimpleDTO::getCreatedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())
            ));

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
                    .sorted(Comparator.comparing(
                            InvestmentProfileVersion::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .map(version -> {
                        String strategyName = (version.getStrategy() != null) ? version.getStrategy().getName() : "N/A";

//                        if (version.getExecutionPlans() != null && !version.getExecutionPlans().isEmpty()) {
//                            vMatchScore = version.getExecutionPlans().get(0).getMatch_score();
//                            if (vMatchScore == null) vMatchScore = 0;
//                        }

                        return ProfileVersionDTO.builder()
                                .investmentProfileVersionId(version.getProfileVersionId())
//                                .matchScore(version.getMatch_score() != 0 ? version.getMatch_score() : 0)
                                .totalCapital(version.getTotalCapital())
                                .name(version.getProfileVersionName())
                                .consciousName(version.getConscious())
                                .wardName(version.getWards() != null ? version.getWards() : new ArrayList<>())
                                .isActive(version.getIsActive())
                                .equity(version.getEquity() != null ? version.getEquity() : 0L)
//                                .expectedRoi(version.getExpectedRoi() != null ? version.getExpectedRoi() : 0L)
//                                .durationYear(version.getDurationYear() != null ? version.getDurationYear() : 0L)
//                                .strategyName(strategyName)
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

            Integer finalMatchScore = 0;
            ExecutionPlanDTO executionPlanDTO = null;
//            if (profileVersion.getExecutionPlans() != null && !profileVersion.getExecutionPlans().isEmpty()) {
//                ExecutionPlan activePlan = profileVersion.getExecutionPlans().get(0);
//                finalMatchScore = activePlan.getMatch_score();
//
//                if (activePlan.getDescription() != null && !activePlan.getDescription().isEmpty()) {
//                    try {
//                        executionPlanDTO = objectMapper.readValue(activePlan.getDescription(), ExecutionPlanDTO.class);
//                    } catch (Exception jsonEx) {
//                        log.error("Error parsing ExecutionPlan JSON for version ID: {}", profileVersionId, jsonEx);
//                    }
//                }
//            }

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

                            return InvestmentCriteriaDTO.builder()
                                    .propertyTypeName(typeName)
                                    .build();
                        })
                        .collect(Collectors.toList());
            }


//            List<String> legalStatusList = Collections.emptyList();

            InvestmentProfileVersionDTO profileDTO = InvestmentProfileVersionDTO.builder()
                    .investmentProfileVersionId(profileVersion.getProfileVersionId())
                    .matchScore(finalMatchScore)
                    .strategyName(profileVersion.getStrategy() != null ? profileVersion.getStrategy().getName() : null)
                    .name(profileVersion.getProfileVersionName())
                    .equity(profileVersion.getEquity())
                    .loanCapital(profileVersion.getLoanCapital())
                    .currentCashflow(profileVersion.getCurrentCashflow())
                    .consciousName(profileVersion.getConscious())
                    .wardName(profileVersion.getWards() != null ? profileVersion.getWards() : new ArrayList<>())
//                    .expectedRoi(profileVersion.getExpectedRoi())
//                    .minProfit(profileVersion.getMinProfit())
//                    .riskToleranceLevel(profileVersion.getRiskToleranceLevel())
////                    .durationYear(profileVersion.getDurationYear())
//                    .startDate(profileVersion.getStartDate())
//                    .investmentType(profileVersion.getInvestmentType())
                    .investmentStrategyDetail(profileVersion.getInvestmentStrategyDetail())
//                    .legalStatus(legalStatusList)
//                    .version(null)
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
    public ResponseEntity<ApiResponse> getInvestmentPlanDetailByVersionId(Integer profileVersionId) {
        try {
            InvestmentProfileVersion profileVersion = investmentProfileVersionRepository.findById(profileVersionId).orElse(null);
            if (profileVersion == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Version_Not_Found", "Không tìm thấy phiên bản kế hoạch với ID: " + profileVersionId));
            }

//            List<InvestmentScenarioDTO> scenarioDTOList = new ArrayList<>();
//            if (profileVersion.getInvestmentScenarios() != null) {
//                scenarioDTOList = profileVersion.getInvestmentScenarios().stream()
//                        .map(scenario -> InvestmentScenarioDTO.builder()
//                                .pkInvestmentScenarioId(scenario.getInvestmentScenarioId())
//                                .enumScenarioType(scenario.getEnumScenarioType())
//                                .decimprofitYield(scenario.getDecimprofitYield())
//                                .textMarketNote(scenario.getTextMarketNote())
//                                .decimmonthlyCashflow(scenario.getDecimmonthlyCashflow())
//                                .decimprobability(scenario.getDecimprobability())
//                                .durationMonths(scenario.getDurationMonths())
//                                .decimpriceGrowthMin(scenario.getDecimpriceGrowthMin())
//                                .decimpriceGrowthMax(scenario.getDecimpriceGrowthMax())
//                                .build())
//                        .collect(Collectors.toList());
//            }

//            Integer finalMatchScore = profileVersion.getMatch_score();
            Long totalCapital = (profileVersion.getEquity() != null ? profileVersion.getEquity() : 0L)
                    + (profileVersion.getLoanCapital() != null ? profileVersion.getLoanCapital() : 0L);

            List<InvestmentCriteriaDTOV2> criteriaDTOV2s = new ArrayList<>();
            if (profileVersion.getInvestmentCriterias() != null) {
                criteriaDTOV2s = profileVersion.getInvestmentCriterias().stream()
                        .map(criteria -> InvestmentCriteriaDTOV2.builder()
                                .investmentCriteriaId(criteria.getInvestmentCriteriaId())
                                .propertyTypeName(criteria.getPropertyType() != null ? criteria.getPropertyType().getName() : "N/A")
                                .proposedPropertyDTOList(criteria.getProposedProperties() == null ? new ArrayList<>() :
                                        criteria.getProposedProperties().stream()
                                                .map(prop -> ProposedPropertyDTO.builder()
                                                        .proposedPropertyId(prop.getProposedPropertyId())
                                                        .listingId(prop.getListingId())
                                                        .proposalType(prop.getProposalType())
                                                        .propertyProjectName(prop.getPropertyProjectName())
                                                        .area(prop.getArea())
                                                        .valuePrice(prop.getValuePrice())
                                                        .description(prop.getDescription())
                                                        .financialMetrics(FinancialMetricsDTO.builder()
                                                                .estimatedProfit(prop.getEstimatedProfit())
//                                                                .estimatedPriceGrowth(prop.getEstimatedPriceGrowth())
                                                                .monthlyRentalCashflow(prop.getMonthlyRentalCashflow())
                                                                .monthlyPrincipalInterest(prop.getMonthlyPrincipalInterest())
                                                                .netCashflow(prop.getNetCashflow())
                                                                .roiPercentage(prop.getRoiPercentage())
                                                                .build())
                                                        .build())
                                                .collect(Collectors.toList())
                                )
                                .build())
                        .collect(Collectors.toList());
            }

            List<InvestmentScenarioDTO> scenarioDTOList = new ArrayList<>();
            if (profileVersion.getInvestmentScenarios() != null) {
                scenarioDTOList = profileVersion.getInvestmentScenarios().stream()
                        .map(scenario -> InvestmentScenarioDTO.builder()
                                .pkInvestmentScenarioId(scenario.getInvestmentScenarioId())
                                .enumScenarioType(scenario.getEnumScenarioType())
                                .decimprofitYield(scenario.getDecimprofitYield())
                                .textMarketNote(scenario.getTextMarketNote())
                                .decimmonthlyCashflow(scenario.getDecimmonthlyCashflow())
                                .decimprobability(null)
                                .durationMonths(scenario.getDurationMonths())
                                .decimpriceGrowthMin(scenario.getDecimpriceGrowthMin())
                                .decimpriceGrowthMax(scenario.getDecimpriceGrowthMax())
                                .build())
                        .collect(Collectors.toList());
            }

            InvestmentPlanDTO finalOutput = InvestmentPlanDTO.builder()
//                    .score(finalMatchScore)
                    .totalCapital(totalCapital)
                    .investmentCriteriaDTOV2s(criteriaDTOV2s)
                    .scenarios(scenarioDTOList)
                    .build();

//            ExecutionPlanDTO executionPlanDTO = null;
//
//            if (profileVersion.getExecutionPlans() != null && !profileVersion.getExecutionPlans().isEmpty()) {
//                ExecutionPlan activePlan = profileVersion.getExecutionPlans().get(0);
//                finalMatchScore = activePlan.getMatch_score();
//
//                executionPlanDTO = ExecutionPlanDTO.builder()
//                        .pkExecutionPlanId(activePlan.getExecutionPlanId())
//                        .totalInvestmentCapital(activePlan.getTotalInvestmentCapital())
//                        .decimloanPercentage(activePlan.getDecimloanPercentage())
//                        .decimmonthlyPayment(activePlan.getDecimmonthlyPayment())
//                        .decimprobability(activePlan.getDecimprobability())
//                        .decimnetCashflow(activePlan.getDecimnetCashflow())
//                        .maxHoldingMonths(activePlan.getMaxHoldingMonths())
//                        .booleanIsLegalClear(activePlan.getBooleanIsLegalClear())
//                        .booleanIsLeverageSafe(activePlan.getBooleanIsLeverageSafe())
//                        .stringLiquidityDurationRange(activePlan.getStringLiquidityDurationRange())
//                        .booleanIsReserveFundEnough(activePlan.getBooleanIsReserveFundEnough())
//                        .textTakeProfitStrategy(activePlan.getTextTakeProfitStrategy())
//                        .textHoldingTimeLimit(activePlan.getTextHoldingTimeLimit())
//                        .textQuickSellAction(activePlan.getTextQuickSellAction())
//                        .build();
//            }
//
//            List<InvestmentPortfolioDTO> portfolioDTOList = new ArrayList<>();
//            if (profileVersion.getInvestmentPortfolios() != null) {
//                int portfolioSeq = 1;
//                for (InvestmentPortfolio ip : profileVersion.getInvestmentPortfolios()) {
//
//                    List<PortfolioAllocationDTO> allocationDTOList = new ArrayList<>();
//
//                    List<PortfolioAllocation> allocationsInDb = portfolioAllocationRepository.findAll().stream()
//                            .filter(pa -> pa.getInvestmentPortfolio() != null &&
//                                    pa.getInvestmentPortfolio().getInvestmentPortfolioId().equals(ip.getInvestmentPortfolioId()))
//                            .collect(Collectors.toList());
//
//                    for (PortfolioAllocation pa : allocationsInDb) {
//                        List<PortfolioAllocationPropertyDTO> propertyDTOList = new ArrayList<>();
//
//                        List<PortfolioAllocationProperty> propertiesInDb = portfolioAllocationPropertyRepository.findAll().stream()
//                                .filter(pap -> pap.getPortfolioAllocation() != null &&
//                                        pap.getPortfolioAllocation().getPortfolioAllocationId().equals(pa.getPortfolioAllocationId()))
//                                .collect(Collectors.toList());
//
//                        for (PortfolioAllocationProperty pap : propertiesInDb) {
//                            Property prop = pap.getProperty();
//
//                            Integer listingId = null;
//                            String title = null;
//                            int area = 0;
//                            double price = 0.0;
//                            String description = null;
//
//                            if (prop != null) {
//                                title = prop.getTitle();
//                                area = prop.getArea() != null ? prop.getArea().intValue() : 0;
//
//                                if (prop.getListings() != null && !prop.getListings().isEmpty()) {
//                                    Listing activeListing = prop.getListings().get(0);
//                                    listingId = activeListing.getListingId();
//                                    price = activeListing.getPrice() != null ? activeListing.getPrice().doubleValue() : 0.0;
//
//                                    String conditionName = (prop.getPropertyCondition() != null)
//                                            ? prop.getPropertyCondition().getName() : "Standard Condition";
//                                    description = "[" + conditionName + "] - " + (activeListing.getDescription() != null ? activeListing.getDescription() : "");
//                                }
//                            }
//
//                            propertyDTOList.add(PortfolioAllocationPropertyDTO.builder()
//                                    .portfolioAllocationPropertyId(listingId)
//                                    .propertyProjectName(title)
//                                    .area(area)
//                                    .valuePrice(price)
//                                    .description(description)
//                                    .build());
//                        }
//
//                        String propertyTypeName = "Real Estate";
//                        if (pa.getPortfolio() != null) {
//                            propertyTypeName = pa.getPortfolio().getName();
//                        }
//
//                        allocationDTOList.add(PortfolioAllocationDTO.builder()
//                                .propertyTypeName(propertyTypeName)
//                                .properties(propertyDTOList)
//                                .build());
//                    }
//
//                    portfolioDTOList.add(InvestmentPortfolioDTO.builder()
//                            .investmentPortfolioId(portfolioSeq++)
//                            .portfolioId(ip.getPortfolio() != null ? ip.getPortfolio().getPortfolioId() : null)
//                            .portfolioName(ip.getPortfolio() != null ? ip.getPortfolio().getName() : "Investment Portfolio")
//                            .percentage(ip.getPercentage())
//                            .capital(ip.getCapital())
//                            .allocations(allocationDTOList)
//                            .build());
//                }
//            }
//
//            InvestmentPlanDTO finalOutput = InvestmentPlanDTO.builder()
//                    .scenarios(scenarioDTOList)
//                    .score(finalMatchScore)
//                    .executionPlan(executionPlanDTO)
//                    .investmentPortfolios(portfolioDTOList)
//                    .build();

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
            Strategy strategy = strategyRepository.findById(request.getStrategyId()).orElse(null);
            if (strategy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Strategy_Not_Found", "Investment strategy not found."));
            }

            Account account = authenUntil.getCurrentUSer();
            if (account.getInvestor() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("INVESTOR_NOT_FOUND", "Investor survey does not exist. Please create to use this function"));
            }

            if (account.getWallet() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("WALLET_NOT_FOUND", "You don't have a wallet. Please deposit money into your wallet to use this feature."));
            }

            Investor investor = account.getInvestor();

            if (investor.getMembershipSubscriptions() == null
                    || investor.getMembershipSubscriptions().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("MEMBERSHIP_NOT_FOUND", "You don't have any membership subscription. Please purchase a plan to use this feature."));
            }

            MembershipSubscription activeSubscription = investor.getMembershipSubscriptions().stream()
                    .filter(sub -> sub.getMembershipSubscriptionEnum_status() != null
                            && sub.getMembershipSubscriptionEnum_status().equals(MembershipSubscriptionEnum.Using)
                            && Boolean.TRUE.equals(sub.getIsActive()))
                    .findFirst()
                    .orElse(null);

            if (activeSubscription == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "SUBSCRIPTION_NOT_ACTIVATED",
                                "You have purchased a membership subscription, but it is not activated yet. Please activate your subscription to use this feature."
                        ));
            }

            if (activeSubscription.getQuantity_using() == null
                    || activeSubscription.getQuantity_using() <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "QUANTITY_EXHAUSTED",
                                "Your membership subscription has run out of usage limit. Please renew or purchase a new plan."
                        ));
            }


            InvestmentPlanDTO finalOutput = callExternalAIServiceToPlan(request, strategy);

            saveNewInvestmentPlan(investor, request, finalOutput, strategy);

            int remainingQuantity = activeSubscription.getQuantity_using() - 1;
            activeSubscription.setQuantity_using(remainingQuantity);

            if (remainingQuantity <= 0) {
                activeSubscription.setIsActive(false);
                activeSubscription.setMembershipSubscriptionEnum_status(
                        MembershipSubscriptionEnum.OutDated
                );
            }

            activeSubscription.setUpdatedAt(LocalDateTime.now());
            membershipSubscriptionRepository.save(activeSubscription);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(
                            finalOutput,
                            "Generate and save complete investment plan successfully"
                    ));

        } catch (Exception e) {
            log.error("Error in generateCompleteInvestmentPlan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateExistingInvestmentPlan(
            Integer currentProfileId,
            UpdateInvestmentPlanRequest request
    ) {
        try {
            InvestmentProfile existingProfile =
                    investmentProfileRepository.findById(currentProfileId).orElse(null);

            if (existingProfile == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "Profile_Not_Found",
                                "Investment profile not found."
                        ));
            }

            Strategy strategy =
                    strategyRepository.findById(request.getStrategyId()).orElse(null);

            if (strategy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "Strategy_Not_Found",
                                "Investment strategy not found."
                        ));
            }

            Account account = authenUntil.getCurrentUSer();

            if (account.getInvestor() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "INVESTOR_NOT_FOUND",
                                "Investor survey does not exist. Please create to use this function"
                        ));
            }

            if (account.getWallet() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "WALLET_NOT_FOUND",
                                "You don't have a wallet. Please deposit money into your wallet to use this feature."
                        ));
            }

            Investor investor = account.getInvestor();

            if (investor.getMembershipSubscriptions() == null
                    || investor.getMembershipSubscriptions().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "MEMBERSHIP_NOT_FOUND",
                                "You don't have any membership subscription. Please purchase a plan to use this feature."
                        ));
            }

            MembershipSubscription activeSubscription =
                    investor.getMembershipSubscriptions().stream()
                            .filter(sub ->
                                    sub.getMembershipSubscriptionEnum_status() != null
                                            && sub.getMembershipSubscriptionEnum_status()
                                            .equals(MembershipSubscriptionEnum.Using)
                                            && Boolean.TRUE.equals(sub.getIsActive())
                            )
                            .findFirst()
                            .orElse(null);

            if (activeSubscription == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "SUBSCRIPTION_NOT_ACTIVATED",
                                "You have purchased a membership subscription, but it is not activated yet. Please activate your subscription to use this feature."
                        ));
            }

            if (activeSubscription.getQuantity_using() == null
                    || activeSubscription.getQuantity_using() <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "QUANTITY_EXHAUSTED",
                                "Your membership subscription has run out of usage limit. Please renew or purchase a new plan."
                        ));
            }

            InvestmentPlanRequest convertedRequest =
                    InvestmentPlanRequest.builder()
                            .equity(request.getEquity())
                            .loanCapital(request.getLoanCapital())
                            .currentCashFlow(request.getCurrentCashFlow())
//                            .riskToleranceLevel(request.getRiskToleranceLevel())
                            .consciousName(request.getConsciousName())
                            .wardNames(request.getWardNames())
//                            .startDate(request.getStartDate())
                            .investmentStrategyDetail(request.getInvestmentStrategyDetail())
                            .criteriaList(request.getCriteriaList())
                            .build();

            InvestmentPlanDTO finalOutput =
                    callExternalAIServiceToPlan(convertedRequest, strategy);

            saveUpdateInvestmentPlan(
                    existingProfile,
                    convertedRequest,
                    finalOutput,
                    strategy
            );

            int remainingQuantity =
                    activeSubscription.getQuantity_using() - 1;

            activeSubscription.setQuantity_using(remainingQuantity);

            if (remainingQuantity <= 0) {
                activeSubscription.setIsActive(false);
                activeSubscription.setMembershipSubscriptionEnum_status(
                        MembershipSubscriptionEnum.OutDated
                );
            }

            activeSubscription.setUpdatedAt(LocalDateTime.now());
            membershipSubscriptionRepository.save(activeSubscription);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(
                            finalOutput,
                            "Update investment plan version successfully"
                    ));

        } catch (Exception e) {
            log.error("Error in updateExistingInvestmentPlan", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(
                            "Server_Error",
                            e.getMessage()
                    ));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> savePlanToDatabaseDirectly(
            SaveInvestmentPlanRequest saveRequest
    ) {
        try {
            if (saveRequest == null
                    || saveRequest.getInputRequest() == null
                    || saveRequest.getAiOutputData() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "Invalid_Payload",
                                "Invalid save request data payload."
                        ));
            }

            Strategy strategy =
                    strategyRepository.findById(
                            saveRequest.getInputRequest().getStrategyId()
                    ).orElse(null);

            if (strategy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "Strategy_Not_Found",
                                "Investment strategy not found."
                        ));
            }

            Account account = authenUntil.getCurrentUSer();
            Investor investor = account != null ? account.getInvestor() : null;

            if (investor == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail(
                                "INVESTOR_NOT_FOUND",
                                "Investor profile not found."
                        ));
            }

            saveNewInvestmentPlan(
                    investor,
                    saveRequest.getInputRequest(),
                    saveRequest.getAiOutputData(),
                    strategy
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            saveRequest.getAiOutputData(),
                            "Investment plan logged and saved successfully"
                    ));

        } catch (Exception e) {
            log.error("Error in savePlanToDatabaseDirectly", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(
                            "Server_Error",
                            e.getMessage()
                    ));
        }
    }

    private void saveNewInvestmentPlan(
            Investor investor,
            InvestmentPlanRequest request,
            InvestmentPlanDTO output,
            Strategy strategy
    ) throws Exception {

        LocalDateTime now = LocalDateTime.now();

        String wardInfo =
                request.getWardNames() != null
                        && !request.getWardNames().isEmpty()
                        ? " " + request.getWardNames().get(0)
                        : "";

        String baseProfileName =
                "Kế hoạch " + strategy.getName() + wardInfo;

        InvestmentProfile profile =
                InvestmentProfile.builder()
                        .investor(investor)
                        .name(baseProfileName)
                        .isActive(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .profileVersions(new ArrayList<>())
                        .build();

        InvestmentProfile savedProfile =
                investmentProfileRepository.save(profile);

        String autoVersionName =
                baseProfileName + " - Phiên bản 1";

        saveProfileVersion(
                savedProfile,
                autoVersionName,
                request,
                output,
                strategy,
                now
        );
    }

    private void saveUpdateInvestmentPlan(
            InvestmentProfile existingProfile,
            InvestmentPlanRequest request,
            InvestmentPlanDTO output,
            Strategy strategy
    ) throws Exception {

        LocalDateTime now = LocalDateTime.now();

        long currentVersionsCount =
                investmentProfileVersionRepository
                        .countByInvestmentProfile_InvestmentProfileId(
                                existingProfile.getInvestmentProfileId()
                        );

        long nextVersionNumber =
                currentVersionsCount + 1;

        String baseProfileName =
                existingProfile.getName() != null
                        ? existingProfile.getName()
                        : "Kế hoạch " + strategy.getName();

        String autoVersionName =
                baseProfileName + " - Phiên bản " + nextVersionNumber;

        saveProfileVersion(
                existingProfile,
                autoVersionName,
                request,
                output,
                strategy,
                now
        );
    }

    private void saveProfileVersion(
            InvestmentProfile profile,
            String versionName,
            InvestmentPlanRequest request,
            InvestmentPlanDTO output,
            Strategy strategy,
            LocalDateTime now
    ) throws Exception {

//        int scoreEvaluated =
//                output != null && output.getScore() != null
//                        ? output.getScore()
//                        : 85;

        long totalCapitalCalculated =
                request.getEquity() + request.getLoanCapital();

        InvestmentProfileVersion versionEntity =
                InvestmentProfileVersion.builder()
                        .investmentProfile(profile)
                        .profileVersionName(versionName)
                        .strategy(strategy)
                        .equity(request.getEquity())
                        .loanCapital(request.getLoanCapital())
                        .currentCashflow(request.getCurrentCashFlow())
                        .conscious(request.getConsciousName())
                        .wards(
                                request.getWardNames() != null
                                        ? request.getWardNames()
                                        : new ArrayList<>()
                        )
                        .totalCapital(totalCapitalCalculated)
//                        .riskToleranceLevel(request.getRiskToleranceLevel())
//                        .startDate(request.getStartDate())
                        .investmentStrategyDetail(
                                request.getInvestmentStrategyDetail()
                        )
//                        .match_score(scoreEvaluated)
                        .isActive(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .investmentCriterias(new ArrayList<>())
                        .investmentScenarios(new ArrayList<>())
                        .build();

        InvestmentProfileVersion savedVersion =
                investmentProfileVersionRepository.save(versionEntity);

        if (output != null && output.getScenarios() != null) {

            for (var scenarioDTO : output.getScenarios()) {

                InvestmentScenario scenarioEntity =
                        InvestmentScenario.builder()
                                .investmentProfileVersion(savedVersion)
                                .scenarioType(
                                        scenarioDTO.getEnumScenarioType()
                                )
                                .expectedReturnRate(
                                        scenarioDTO.getDecimprofitYield()
                                )
                                .isActive(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .enumScenarioType(
                                        scenarioDTO.getEnumScenarioType()
                                )
                                .decimprofitYield(
                                        scenarioDTO.getDecimprofitYield()
                                )
                                .decimmonthlyCashflow(
                                        scenarioDTO.getDecimmonthlyCashflow()
                                )
                                .textMarketNote(
                                        scenarioDTO.getTextMarketNote()
                                )
                                .durationMonths(
                                        scenarioDTO.getDurationMonths()
                                )
                                .decimpriceGrowthMin(
                                        scenarioDTO.getDecimpriceGrowthMin()
                                )
                                .decimpriceGrowthMax(
                                        scenarioDTO.getDecimpriceGrowthMax()
                                )
                                .build();

                investmentScenarioRepository.save(scenarioEntity);
            }
        }

        if (request.getCriteriaList() != null
                && output != null
                && output.getInvestmentCriteriaDTOV2s() != null) {

            int index = 0;

            for (var critReq : request.getCriteriaList()) {

                PropertyType pType =
                        propertyTypeRepository.findById(
                                critReq.getPropertyTypeId()
                        ).orElse(null);

                InvestmentCriteria criteriaEntity =
                        new InvestmentCriteria();

                criteriaEntity.setInvestmentProfileVersion(savedVersion);
                criteriaEntity.setPropertyType(pType);

                InvestmentCriteria savedCriteria =
                        investmentCriteriaRepository.save(criteriaEntity);

                if (index < output.getInvestmentCriteriaDTOV2s().size()) {

                    var critDTO =
                            output.getInvestmentCriteriaDTOV2s().get(index);

                    critDTO.setInvestmentCriteriaId(
                            savedCriteria.getInvestmentCriteriaId()
                    );

                    if (pType != null) {
                        critDTO.setPropertyTypeName(
                                pType.getName()
                        );
                    }

                    if (critDTO.getProposedPropertyDTOList() != null) {

                        for (var propDTO :
                                critDTO.getProposedPropertyDTOList()) {

                            FinancialMetricsDTO mDTO =
                                    propDTO.getFinancialMetrics();

                            ProposedProperty propEntity =
                                    new ProposedProperty();

                            propEntity.setInvestmentCriteria(
                                    savedCriteria
                            );

                            propEntity.setListingId(
                                    propDTO.getListingId() != null
                                            ? propDTO.getListingId()
                                            : 0
                            );

                            propEntity.setProposalType(
                                    propDTO.getProposalType()
                            );

                            propEntity.setPropertyProjectName(
                                    propDTO.getPropertyProjectName()
                            );

                            propEntity.setArea(
                                    propDTO.getArea()
                            );

                            propEntity.setValuePrice(
                                    propDTO.getValuePrice()
                            );

                            propEntity.setDescription(
                                    propDTO.getDescription()
                            );

                            propEntity.setEstimatedProfit(
                                    mDTO != null
                                            ? mDTO.getEstimatedProfit()
                                            : null
                            );

//                            propEntity.setEstimatedPriceGrowth(
//                                    mDTO != null
//                                            ? mDTO.getEstimatedPriceGrowth()
//                                            : null
//                            );

                            propEntity.setMonthlyRentalCashflow(
                                    mDTO != null
                                            ? mDTO.getMonthlyRentalCashflow()
                                            : null
                            );

                            propEntity.setMonthlyPrincipalInterest(
                                    mDTO != null
                                            ? mDTO.getMonthlyPrincipalInterest()
                                            : null
                            );

                            propEntity.setNetCashflow(
                                    mDTO != null
                                            ? mDTO.getNetCashflow()
                                            : null
                            );

                            propEntity.setRoiPercentage(
                                    mDTO != null
                                            ? mDTO.getRoiPercentage()
                                            : null
                            );

                            propEntity.setCreatedAt(now);

                            ProposedProperty savedProperty =
                                    proposedPropertyRepository.save(
                                            propEntity
                                    );

                            propDTO.setProposedPropertyId(
                                    savedProperty.getProposedPropertyId()
                            );
                        }
                    }
                }

                index++;
            }
        }
    }
    private InvestmentPlanDTO callExternalAIServiceToPlan(
            InvestmentPlanRequest request,
            Strategy strategy
    ) throws Exception {

        long totalCapital = request.getEquity() + request.getLoanCapital();

        List<InvestmentCriteriaDTOV2> criteriaList = new ArrayList<>();
        List<Map<String, Object>> aiCriteria = new ArrayList<>();

        for (var crit : request.getCriteriaList()) {

            List<ProposedPropertyDTO> properties = new ArrayList<>();
            List<Map<String, Object>> aiProperties = new ArrayList<>();

            // 1. Tìm BĐS theo Tổng vốn
            List<Listing> totalListings = listingRepository.findListingsByCriteria(
                    crit.getPropertyTypeId(),
                    request.getWardNames(),
                    totalCapital
            );

            if (!totalListings.isEmpty()) {
                addProperty(
                        totalListings.get(0),
                        "TOTAL_CAPITAL_BASED",
                        properties,
                        aiProperties
                );
            }

            Integer firstListingId = !totalListings.isEmpty() ? totalListings.get(0).getListingId() : null;

            // 2. Tìm BĐS theo Vốn tự có
            List<Listing> equityListings = listingRepository.findListingsByCriteria(
                    crit.getPropertyTypeId(),
                    request.getWardNames(),
                    request.getEquity()
            );

            // Filter tránh trùng với BĐS 1 & Fallback an toàn
            Listing secondListing = equityListings.stream()
                    .filter(l -> !Objects.equals(l.getListingId(), firstListingId))
                    .findFirst()
                    .orElseGet(() -> totalListings.stream()
                            .filter(l -> !Objects.equals(l.getListingId(), firstListingId))
                            .findFirst()
                            .orElse(null)
                    );

            if (secondListing != null) {
                String proposalType = (secondListing.getPrice() <= request.getEquity())
                        ? "EQUITY_BASED"
                        : "ALTERNATIVE_STRATEGY";

                addProperty(
                        secondListing,
                        proposalType,
                        properties,
                        aiProperties
                );
            }

            criteriaList.add(
                    InvestmentCriteriaDTOV2.builder()
                            .proposedPropertyDTOList(properties)
                            .build()
            );

            aiCriteria.add(
                    Map.of(
                            "propertyTypeId", crit.getPropertyTypeId(),
                            "properties", aiProperties
                    )
            );
        }

        String strategyName = (strategy != null && strategy.getName() != null)
                ? strategy.getName()
                : "";

        String propertiesJson = objectMapper.writeValueAsString(aiCriteria);

        String prompt =
                """
                Bạn là chuyên gia phân tích tài chính và bất động sản tại TP.HCM.
                Hãy thực hiện tính toán tài chính chi tiết dựa trên các quy tắc chiến lược được quy định bên dưới và trả về đúng định dạng JSON theo schema đã cho.
    
                QUY TẮC THỜI GIAN, CHI PHÍ VÀ LỢI NHUẬN THEO CHIẾN LƯỢC:
    
                1. Chiến lược "Đầu Cơ Lướt Sóng":
                   - Thời gian đầu tư: 1–3 tháng.
                   - Lợi nhuận kỳ vọng cố định: selectedProfitRate = 0.15 (15%% / thương vụ).
                   - Dòng tiền thuê: Không có (monthlyRentalCashflow = 0).
                   - Chi phí phát sinh: Không có (additionalCost = 0).
                   - Công thức tính lợi nhuận: estimatedProfit = valuePrice * 0.15.
    
                2. Chiến lược "Mua Sửa Bán":
                   - Thời gian đầu tư: 6–9 tháng.
                   - Lợi nhuận kỳ vọng cố định: selectedProfitRate = 0.20 (20%% / thương vụ).
                   - Dòng tiền thuê: Không có (monthlyRentalCashflow = 0).
                   - Chi phí cải tạo/sửa chữa mặc định: additionalCost = valuePrice * 0.05 (5%% giá trị BĐS).
                   - Công thức tính lợi nhuận: estimatedProfit = (valuePrice * 0.20) - additionalCost.
    
                3. Chiến lược "BĐS Dòng Tiền":
                   - Thời gian đầu tư cố định: 3 năm (investmentYears = 3).
                   - Lợi nhuận tăng giá cố định: selectedProfitRate = 0.05 (5%% / năm).
                   - Thu nhập thuê năm: annualRentalIncome = monthlyRentalCashflow * 12.
                   - Công thức tính lợi nhuận tổng 3 năm: estimatedProfit = valuePrice * ((1 + 0.05 + (annualRentalIncome / valuePrice)) ^ 3 - 1).
    
                CHIẾN LƯỢC ĐANG ÁP DỤNG (CURRENT STRATEGY):
                %s
    
                QUY TẮC TÍNH TOÁN TÀI CHÍNH TỔNG QUÁT:
    
                1. Dòng Tiền Cho Thuê Hàng Tháng (monthlyRentalCashflow):
                   - ĐÁNH GIÁ VÀ ĐỐI CHIẾU TỶ SUẤT CHO THUÊ NĂM (Annual Rental Yield) dựa vào bộ tiêu chí: hiện trạng bất động sản (propertyCondition), vị trí địa lý (wardName) và giá trị bất động sản (valuePrice).
                   - NGUỒN DỮ LIỆU THAM CHIẾU (Thị trường TP.HCM): CBRE, Savills, Batdongsan.com.vn, VARS.
                   - MA TRẬN TỶ SUẤT THUÊ NĂM CHUẨN (Dành cho TP.HCM):
                     * BĐS mới / Đủ nội thất / Chất lượng tốt (propertyCondition = "Mới", "Tốt", "Hoàn thiện"): Tỷ suất 4.5%% - 5.5%% / năm.
                     * BĐS cũ / Cần sửa chữa / Bình thường (propertyCondition = "Cũ", "Cần sửa chữa", "Bình thường"): Tỷ suất 3.0%% - 4.0%% / năm.
                     * Vị trí Phường trung tâm (wardName thuộc Q1, Q3, Thảo Điền...): Tỷ suất 2.5%% - 3.5%% / năm do giá trị BĐS (valuePrice) cao.
                     * Vị trí Phường tập trung đông nhu cầu thuê (wardName thuộc Bình Thạnh, Q7, Q10, Tân Bình...): Tỷ suất 4.5%% - 5.5%% / năm.
                   - CÔNG THỨC: monthlyRentalCashflow = (valuePrice * Annual Rental Yield) / 12. (Nếu là chiến lược "Đầu Cơ Lướt Sóng" hoặc "Mua Sửa Bán" thì gán thẳng = 0).
    
                2. Tiền Trả Gốc Và Lãi Hàng Tháng (monthlyPrincipalInterest):
                   - Công thức khoản vay (Amortization): P * [r * (1 + r)^n] / [(1 + r)^n - 1]
                     Trong đó: P = loanCapital (nếu loanCapital <= 0 thì monthlyPrincipalInterest = 0), r = (lãi suất năm trung bình ngân hàng Big4 năm 2026 / 12), n = 240 tháng.
    
                3. Dòng Tiền Ròng Hàng Tháng (netCashflow):
                   - Với chiến lược "BĐS Dòng Tiền": netCashflow = monthlyRentalCashflow - monthlyPrincipalInterest.
                   - Với "Đầu Cơ Lướt Sóng" và "Mua Sửa Bán": netCashflow = 0.
    
                4. Tỷ Lệ ROI (roiPercentage):
                   - roiPercentage = (estimatedProfit / valuePrice) * 100.
    
                RÀNG BUỘC OUTPUT:
                - Không tính estimatedPriceGrowth.
                - Không trả về điểm tương thích (match score).
                - Phải tạo đúng 3 kịch bản theo thứ tự tên enumScenarioType: "xu hướng tăng", "trung bình", "xu hướng giảm".
                - CHỈ TRẢ VỀ JSON HỢP LỆ.
    
                DANH SÁCH BẤT ĐỘNG SẢN ĐƯỢC CHỌN (SELECTED PROPERTIES):
                %s
                """.formatted(
                        strategyName,
                        propertiesJson
                );

        Schema financialMetricsSchema = Schema.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "estimatedProfit", Schema.builder().type("NUMBER").build(),
                        "monthlyRentalCashflow", Schema.builder().type("NUMBER").build(),
                        "monthlyPrincipalInterest", Schema.builder().type("NUMBER").build(),
                        "netCashflow", Schema.builder().type("NUMBER").build(),
                        "roiPercentage", Schema.builder().type("NUMBER").build()
                ))
                .required(List.of(
                        "estimatedProfit",
                        "monthlyRentalCashflow",
                        "monthlyPrincipalInterest",
                        "netCashflow",
                        "roiPercentage"
                ))
                .build();

        Schema propertySchema = Schema.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "listingId", Schema.builder().type("INTEGER").build(),
                        "financialMetrics", financialMetricsSchema
                ))
                .required(List.of("listingId", "financialMetrics"))
                .build();

        Schema criteriaSchema = Schema.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "proposedPropertyDTOList", Schema.builder().type("ARRAY").items(propertySchema).build()
                ))
                .required(List.of("proposedPropertyDTOList"))
                .build();

        Schema scenarioSchema = Schema.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "enumScenarioType", Schema.builder().type("STRING").build(),
                        "decimprofitYield", Schema.builder().type("NUMBER").build(),
                        "decimmonthlyCashflow", Schema.builder().type("NUMBER").build(),
                        "decimprobability", Schema.builder().type("NUMBER").build(),
                        "textMarketNote", Schema.builder().type("STRING").build(),
                        "durationMonths", Schema.builder().type("INTEGER").build()
                ))
                .required(List.of(
                        "enumScenarioType",
                        "decimprofitYield",
                        "decimmonthlyCashflow",
                        "decimprobability",
                        "textMarketNote",
                        "durationMonths"
                ))
                .build();

        Schema dataSchema = Schema.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "scenarios", Schema.builder().type("ARRAY").minItems(3L).maxItems(3L).items(scenarioSchema).build(),
                        "investmentCriteriaDTOV2s", Schema.builder().type("ARRAY").items(criteriaSchema).build()
                ))
                .required(List.of("scenarios", "investmentCriteriaDTOV2s"))
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

        GenerateContentResponse response = geminiClient.models.generateContent(
                "gemini-2.5-flash",
                prompt,
                config
        );

        String json = response.text();

        if (json == null || json.isBlank()) {
            throw new IllegalStateException("AI returned an empty response.");
        }

        json = json.trim();
        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }

        JsonNode root = objectMapper.readTree(json.trim());
        JsonNode data = root.has("data") ? root.path("data") : root;
        JsonNode aiCriteriaV2 = data.path("investmentCriteriaDTOV2s");

        for (int i = 0; i < criteriaList.size(); i++) {

            var hardCriteria = criteriaList.get(i);
            JsonNode aiCrit = aiCriteriaV2.path(i);
            JsonNode aiPropertiesNode = aiCrit.path("proposedPropertyDTOList");

            List<ProposedPropertyDTO> resultProperties = new ArrayList<>();

            for (int j = 0; j < aiPropertiesNode.size(); j++) {

                JsonNode propNode = aiPropertiesNode.get(j);
                Long listingId = propNode.path("listingId").asLong();

                ProposedPropertyDTO matchedDto = hardCriteria.getProposedPropertyDTOList().stream()
                        .filter(p -> p.getListingId().equals(listingId))
                        .findFirst()
                        .orElse(null);

                if (matchedDto != null) {
                    JsonNode metrics = propNode.path("financialMetrics");

                    matchedDto.setFinancialMetrics(
                            FinancialMetricsDTO.builder()
                                    .estimatedProfit(Math.round(metrics.path("estimatedProfit").asDouble(0)))
                                    .monthlyRentalCashflow(Math.round(metrics.path("monthlyRentalCashflow").asDouble(0)))
                                    .monthlyPrincipalInterest(Math.round(metrics.path("monthlyPrincipalInterest").asDouble(0)))
                                    .netCashflow(Math.round(metrics.path("netCashflow").asDouble(0)))
                                    .roiPercentage(metrics.path("roiPercentage").asDouble(0))
                                    .build()
                    );

                    resultProperties.add(matchedDto);
                }
            }

            hardCriteria.setProposedPropertyDTOList(resultProperties);
        }

        List<InvestmentScenarioDTO> scenarios = new ArrayList<>();
        JsonNode scenarioArray = data.path("scenarios");

        if (!scenarioArray.isArray() || scenarioArray.size() != 3) {
            throw new IllegalStateException("AI must return exactly 3 investment scenarios.");
        }

        List<String> scenarioOrder = List.of("xu hướng tăng", "trung bình", "xu hướng giảm");

        for (int i = 0; i < 3; i++) {
            JsonNode node = scenarioArray.get(i);
            String type = node.path("enumScenarioType").asText();

            if (!scenarioOrder.get(i).equals(type)) {
                throw new IllegalStateException("Invalid investment scenario order.");
            }

            scenarios.add(
                    InvestmentScenarioDTO.builder()
                            .enumScenarioType(type)
                            .decimprofitYield(node.path("decimprofitYield").asDouble(0))
                            .decimmonthlyCashflow(node.path("decimmonthlyCashflow").asDouble(0))
                            .decimprobability(node.path("decimprobability").asDouble(0))
                            .textMarketNote(node.path("textMarketNote").asText(""))
                            .durationMonths(node.path("durationMonths").asInt(0))
                            .build()
            );
        }

        return InvestmentPlanDTO.builder()
                .totalCapital(totalCapital)
                .investmentCriteriaDTOV2s(criteriaList)
                .scenarios(scenarios)
                .build();
    }

    private void addProperty(
            Listing listing,
            String proposalType,
            List<ProposedPropertyDTO> properties,
            List<Map<String, Object>> aiProperties
    ) {
        Property property = listing.getProperty();

        String condition =
                property != null
                        && property.getPropertyCondition() != null
                        ? property.getPropertyCondition().getName()
                        : "Unknown";

        String ward =
                property != null
                        && property.getLocation() != null
                        && property.getLocation().getWard() != null
                        ? property.getLocation().getWard().getName()
                        : "Unknown";

        properties.add(
                ProposedPropertyDTO.builder()
                        .listingId(listing.getListingId())
                        .proposalType(proposalType)
                        .propertyProjectName(listing.getTitle())
                        .area(
                                property != null
                                        && property.getArea() != null
                                        ? property.getArea().intValue()
                                        : 0
                        )
                        .valuePrice(
                                listing.getPrice() != null
                                        ? listing.getPrice().doubleValue()
                                        : 0.0
                        )
                        .description(listing.getDescription())
                        .build()
        );

        aiProperties.add(
                Map.of(
                        "listingId",
                        listing.getListingId(),
                        "title",
                        listing.getTitle(),
                        "valuePrice",
                        listing.getPrice() != null ? listing.getPrice() : 0,
                        "area",
                        property != null && property.getArea() != null
                                ? property.getArea()
                                : 0.0,
                        "propertyCondition",
                        condition,
                        "wardName",
                        ward,
                        "proposalType",
                        proposalType
                )
        );
    }

//    @Override
//    @Transactional
//    public ResponseEntity<ApiResponse> generateCompleteInvestmentPlan(InvestmentPlanRequest request) {
//        try {
//            Strategy strategy = strategyRepository.findById(request.getStrategy_id()).orElse(null);
//            if (strategy == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("Strategy_Not_Found", "Investment strategy not found."));
//            }
//
//            Account account = authenUntil.getCurrentUSer();
//            if(account.getInvestor() == null){
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail(null, "Investor survey does not exist. Please create to use this function"));
//            }
//
//            if (account.getWallet() == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("WALLET_NOT_FOUND", "You don't have a wallet. Please deposit money into your wallet to use this feature."));
//            }
//
//            Investor investor = account.getInvestor();
//            if (investor == null || investor.getMembershipSubscriptions() == null || investor.getMembershipSubscriptions().isEmpty()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("MEMBERSHIP_NOT_FOUND", "You don't have any membership subscription. Please purchase a plan to use this feature."));
//            }
//
//            MembershipSubscription activeSubscription = investor.getMembershipSubscriptions().stream()
//                    .filter(sub -> sub.getMembershipSubscriptionEnum_status() != null
//                            && sub.getMembershipSubscriptionEnum_status().equals(MembershipSubscriptionEnum.Using)
//                            && Boolean.TRUE.equals(sub.getIsActive()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (activeSubscription == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("SUBSCRIPTION_NOT_ACTIVATED", "You have purchased a membership subscription, but it is not activated yet. Please activate your subscription to use this feature."));
//            }
//
//            if (activeSubscription.getQuantity_using() == null || activeSubscription.getQuantity_using() <= 0) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("QUANTITY_EXHAUSTED", "Your membership subscription has run out of usage limit. Please renew or purchase a new plan."));
//            }
//
////            final String inputName = request.getName() != null ? request.getName().trim() : "";
////            if (!inputName.isEmpty()) {
////                final Integer currentInvestorId = account.getInvestor().getInvestorId();
////
////                boolean isNameExists = investmentProfileRepository.findAll().stream()
////                        .anyMatch(p -> p.getInvestor() != null
////                                && p.getInvestor().getInvestorId().equals(currentInvestorId)
////                                && p.getName() != null
////                                && p.getName().equalsIgnoreCase(inputName)
////                                && p.getIsActive() == true);
////
////                if (isNameExists) {
////                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
////                            .body(ApiResponse.fail("Name_Already_Exists", "The investment plan name '" + inputName + "' already exists. Please choose another name."));
////                }
////            }
//
//            List<InvestmentPortfolioDTO> portfolios = processStage1Portfolios(request, strategy);
//            processStage2EnrichProperties(request, portfolios);
//            InvestmentPlanDTO finalOutput = processStage3ScenariosAndExecution(request, portfolios, account.getInvestor());
//            saveInvestmentPlanToDatabase(request, finalOutput, strategy);
//
//            activeSubscription.setQuantity_using(activeSubscription.getQuantity_using() - 1);
//
//            int remainingQuantity = activeSubscription.getQuantity_using() - 1;
//            activeSubscription.setQuantity_using(remainingQuantity);
//
//
//            if (remainingQuantity <= 0) {
//                activeSubscription.setIsActive(false);
//                activeSubscription.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.OutDated);
//            }
//
//            activeSubscription.setUpdatedAt(LocalDateTime.now());
//            membershipSubscriptionRepository.save(activeSubscription);
//
//            return ResponseEntity.status(HttpStatus.OK)
//                    .body(ApiResponse.success(finalOutput, "Generate and save complete investment plan successfully"));
//        } catch (Exception e) {
//            log.error("Error in generateCompleteInvestmentPlan", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
//        }
//    }
//
//    @Override
//    @Transactional
//    public ResponseEntity<ApiResponse> savePlanToDatabaseDirectly(SaveInvestmentPlanRequest saveRequest) {
//        try {
//            if (saveRequest == null || saveRequest.getInputRequest() == null || saveRequest.getAiOutputData() == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("Invalid_Payload", "Invalid save request data payload."));
//            }
//
//            Strategy strategy = strategyRepository.findById(saveRequest.getInputRequest().getStrategy_id()).orElse(null);
//            if (strategy == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("Strategy_Not_Found", "Investment strategy not found."));
//            }
//
//            saveInvestmentPlanToDatabase(saveRequest.getInputRequest(), saveRequest.getAiOutputData(), strategy);
//
//            return ResponseEntity.status(HttpStatus.CREATED)
//                    .body(ApiResponse.success(null, "Investment plan logged and saved successfully"));
//        } catch (Exception e) {
//            log.error("Error in savePlanToDatabaseDirectly", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
//        }
//    }
//
//    private List<InvestmentPortfolioDTO> processStage1Portfolios(InvestmentPlanRequest request, Strategy strategy) {
//        List<StrategyPortfolio> activeRelations = strategy.getStrategyPortfolios().stream()
//                .filter(sp -> sp.getIsActive() != null && sp.getIsActive())
//                .collect(Collectors.toList());
//
//        if (activeRelations.isEmpty()) {
//            throw new RuntimeException("No active portfolios linked to this strategy.");
//        }
//
//        List<Map<String, Object>> portfolioInputList = new ArrayList<>(activeRelations.size());
//        for (StrategyPortfolio relation : activeRelations) {
//            Portfolio p = relation.getPortfolio();
//            Map<String, Object> map = new HashMap<>();
//            map.put("portfolioId", p.getPortfolioId());
//            map.put("portfolioName", p.getName());
//            portfolioInputList.add(map);
//        }
//
//        String promptStage1 = String.format(
//                "Dựa trên nguồn vốn của khách hàng:\n- Vốn tự có (Equity): %d\n- Vốn vay (Loan Capital): %d\n- Quỹ dự phòng (Reserve Fund): %d\n" +
//                        "Hãy phân bổ nguồn vốn này vào các danh mục đầu tư mục tiêu sau (Tổng phần trăm phân bổ bắt buộc phải bằng 100%%): %s.\n" +
//                        "YÊU CẦU BẮT BUỘC: Toàn bộ các nội dung văn bản, mô tả, phân tích phải được viết hoàn toàn bằng TIẾNG VIỆT.",
//                request.getEquity(), request.getLoanCapital(), request.getReserveFund(), portfolioInputList.toString()
//        );
//
//        GenerateContentConfig configStage1 = GenerateContentConfig.builder()
//                .responseMimeType("application/json")
//                .responseSchema(Schema.builder()
//                        .type("ARRAY")
//                        .items(Schema.builder()
//                                .type("OBJECT")
//                                .properties(Map.of(
//                                        "portfolioId", Schema.builder().type("INTEGER").build(),
//                                        "percentage", Schema.builder().type("INTEGER").build(),
//                                        "capital", Schema.builder().type("NUMBER").build()
//                                ))
//                                .required(List.of("portfolioId", "percentage", "capital"))
//                                .build())
//                        .build())
//                .build();
//
//        int retryCount = 0;
//        GenerateContentResponse response = null;
//
//        while (retryCount < 5) {
//            try {
//                response = geminiClient.models.generateContent("gemini-2.5-flash", promptStage1, configStage1);
//                break;
//            } catch (Exception e) {
//                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
//                // Bổ sung bắt cả lỗi 429 quá tải lượt gọi lẫn lỗi 503 từ server Google
//                if (errorMsg.contains("429") || errorMsg.contains("503") || errorMsg.contains("Unavailable") || errorMsg.contains("Quota exceeded") || errorMsg.contains("rate-limits")) {
//                    retryCount++;
//                    try {
//                             Thread.sleep(8000);
//                    } catch (InterruptedException ie) {
//                        Thread.currentThread().interrupt();
//                        throw new RuntimeException(ie);
//                    }
//                } else {
//                    throw e;
//                }
//            }
//        }
//
//        if (response == null) {
//            throw new RuntimeException("Gemini API đang bận hoặc quá hạn mức (429/503). Vui lòng thử lại sau vài giây.");
//        }
//
//        try {
//            List<Map<String, Object>> parsedAllocations = objectMapper.readValue(response.text().trim(), new TypeReference<List<Map<String, Object>>>() {});
//            List<InvestmentPortfolioDTO> investmentPortfolios = new ArrayList<>(parsedAllocations.size());
//            int seq = 1;
//
//            Map<Integer, String> portfolioNameMap = activeRelations.stream()
//                    .collect(Collectors.toMap(r -> r.getPortfolio().getPortfolioId(), r -> r.getPortfolio().getName(), (v1, v2) -> v1));
//
//            for (Map<String, Object> item : parsedAllocations) {
//                Integer pId = (Integer) item.get("portfolioId");
//                Integer pPercentage = (Integer) item.get("percentage");
//                Double pCapital = ((Number) item.get("capital")).doubleValue();
//                String pName = portfolioNameMap.getOrDefault(pId, "Investment Portfolio");
//
//                investmentPortfolios.add(InvestmentPortfolioDTO.builder()
//                        .investmentPortfolioId(seq++)
//                        .portfolioId(pId)
//                        .portfolioName(pName)
//                        .percentage(pPercentage)
//                        .capital(pCapital)
//                        .allocations(new ArrayList<>())
//                        .build());
//            }
//            return investmentPortfolios;
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void processStage2EnrichProperties(InvestmentPlanRequest request, List<InvestmentPortfolioDTO> portfolios) {
//        List<CriteriaRequest> criteriaList = request.getCriteriaList();
//        if (criteriaList == null || criteriaList.isEmpty()) {
//            return;
//        }
//
//        List<Integer> typeIds = criteriaList.stream()
//                .map(CriteriaRequest::getPropertyTypeId)
//                .filter(Objects::nonNull)
//                .distinct()
//                .collect(Collectors.toList());
//
//        Map<Integer, String> propertyTypeCache = propertyTypeRepository.findAllById(typeIds).stream()
//                .collect(Collectors.toMap(PropertyType::getPropertyTypeId, PropertyType::getName, (v1, v2) -> v1));
//
//        int totalTypes = criteriaList.size();
//        String ward = request.getWardName();
//
//        for (InvestmentPortfolioDTO portfolio : portfolios) {
//            double capitalPerType = portfolio.getCapital() / totalTypes;
//            List<PortfolioAllocationDTO> allocations = new ArrayList<>(totalTypes);
//
//            for (CriteriaRequest crit : criteriaList) {
//                Integer typeId = crit.getPropertyTypeId();
//                String currentPropertyTypeName = propertyTypeCache.getOrDefault(typeId, "Real Estate");
//
//                PortfolioAllocationDTO allocationDTO = PortfolioAllocationDTO.builder()
//                        .propertyTypeName(currentPropertyTypeName)
//                        .properties(new ArrayList<>())
//                        .build();
//
//                List<PortfolioAllocationPropertyDTO> dbRealProperties = queryRealWarehouseProperties(
//                        capitalPerType,
//                        ward,
//                        typeId
//                );
//
//                allocationDTO.setProperties(dbRealProperties);
//                allocations.add(allocationDTO);
//            }
//            portfolio.setAllocations(allocations);
//        }
//    }
//
//    private List<PortfolioAllocationPropertyDTO> queryRealWarehouseProperties(Double capitalPerType, String ward, Integer propertyTypeId) {
//        Long maxPriceAllowed = capitalPerType.longValue();
//
//        List<Listing> matchedListings = listingRepository.findRealPropertiesByAiStrategy(
//                ward, propertyTypeId, maxPriceAllowed
//        );
//
//        if (matchedListings.isEmpty()) {
////            return Collections.singletonList(PortfolioAllocationPropertyDTO.builder()
////                    .portfolioAllocationPropertyId(null)
////                    .propertyProjectName(null)
////                    .area(0)
////                    .valuePrice(0.0)
////                    .description(null)
////                    .build());
//
//            return Collections.singletonList(PortfolioAllocationPropertyDTO.builder()
//                    .portfolioAllocationPropertyId(null)
//                    .propertyProjectName(null)
//                    .area(null)
//                    .valuePrice(null)
//                    .description(null)
//                    .build());
//        }
//
//        int limit = Math.min(matchedListings.size(), 2);
//        List<PortfolioAllocationPropertyDTO> resultList = new ArrayList<>(limit);
//
//        for (int i = 0; i < limit; i++) {
//            Listing listing = matchedListings.get(i);
//            Property property = listing.getProperty();
//
//            String conditionName = (property != null && property.getPropertyCondition() != null)
//                    ? property.getPropertyCondition().getName()
//                    : "Standard Condition";
//
//            String listingDesc = listing.getDescription() != null ? listing.getDescription() : "";
//            String finalDescription = "[" + conditionName + "] - " + listingDesc;
//
//            resultList.add(PortfolioAllocationPropertyDTO.builder()
//                    .portfolioAllocationPropertyId(listing.getListingId())
//                    .propertyProjectName(property != null ? property.getTitle() : null)
//                    .area((property != null && property.getArea() != null) ? property.getArea().intValue() : 0)
//                    .valuePrice(listing.getPrice() != null ? listing.getPrice().doubleValue() : 0.0)
//                    .description(finalDescription)
//                    .build());
//        }
//
//        return resultList;
//    }
//
//    private InvestmentPlanDTO processStage3ScenariosAndExecution(InvestmentPlanRequest request, List<InvestmentPortfolioDTO> portfolios, Investor investor) {
//        String cleanTreeJson = "";
//        try {
//            cleanTreeJson = objectMapper.writeValueAsString(portfolios);
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
//
//        String investorSurveyContext = String.format(
//                "--- THÔNG TIN KHẢO SÁT KHẨU VỊ NHÀ ĐẦU TƯ ---\n" +
//                        "- Kinh nghiệm đầu tư: %s\n" +
//                        "- Có thu nhập ổn định: %s\n" +
//                        "- Mục tiêu đầu tư: %s\n" +
//                        "- Tiêu chí ưu tiên: %s\n" +
//                        "- Phong cách đầu tư: %s\n" +
//                        "- Kỳ vọng lợi nhuận: %s\n" +
//                        "- Loại hình BĐS ưa thích: %s\n" +
//                        "- Yếu tố quyết định xuống tiền: %s\n" +
//                        "- Khả năng tự quản lý: %s\n" +
//                        "- Phương thức đầu tư: %s\n\n",
//                investor.getInvestmentExperience(),
//                investor.getStableIncome() != null && investor.getStableIncome() ? "Có" : "Không",
//                investor.getInvestmentGoal(),
//                investor.getInvestmentPriority(),
//                investor.getInvestmentStyle(),
//                investor.getReturnExpectation(),
//                investor.getPropertyPreference(),
//                investor.getDecisionFactor(),
//                investor.getManagementAbility(),
//                investor.getInvestmentMethod()
//        );
//
//        String promptStage3 = "Hãy đóng vai trò là một chuyên gia phân tích tài chính bất động sản chuyên nghiệp.\n" +
//                "Dưới đây là kết quả khảo sát khẩu vị rủi ro và phong cách của nhà đầu tư này:\n" + investorSurveyContext +
//                "Nhiệm vụ của bạn là đối chiếu dữ liệu khảo sát trên với cấu trúc danh mục và phân bổ bất động sản thực tế sau đây: " + cleanTreeJson + "\n\n" +
//                "YÊU CẦU ĐẦU RA BẮT BUỘC:\n" +
//                "1. Tại trường 'score': Hãy phân tích xem danh mục BĐS thực tế thu được có khớp với gu, mong muốn và năng lực tài chính của nhà đầu tư không. Hãy chấm một điểm số đại diện cho mức độ PHÙ HỢP VÀ KHẢ THI của phương án trên thang điểm từ 0 đến 100.\n" +
//                "2. Toàn bộ nội dung thông tin, phân tích, nhận định thị trường, chiến lược hành động, giải thích phải viết bằng TIẾNG VIỆT 100% (Tuyệt đối không trộn lẫn từ ngữ tiếng Anh).\n" +
//                "3. Tại mảng 'scenarios' (Danh sách kịch bản), bạn BẮT BUỘC phải tạo ra chính xác 3 phần tử kịch bản. Trường 'enumScenarioType' của các kịch bản này CHỈ ĐƯỢC PHÉP đặt tên theo đúng 3 định dạng cố định sau:\n" +
//                "   - 'xu hướng tăng'\n" +
//                "   - 'trung bình'\n" +
//                "   - 'xu hướng giảm'\n" +
//                "Trả về kết quả phân tích thị trường toàn diện này dưới định dạng cấu trúc JSON chính xác theo Schema quy định.";
//
//        GenerateContentConfig configStage3 = GenerateContentConfig.builder()
//                .responseMimeType("application/json")
//                .responseSchema(Schema.builder()
//                        .type("OBJECT")
//                        .properties(Map.of(
//                                "score", Schema.builder().type("INTEGER").build(), // Nhận diện trường score ở gốc JSON
//                                "scenarios", Schema.builder()
//                                        .type("ARRAY")
//                                        .items(Schema.builder()
//                                                .type("OBJECT")
//                                                .properties(Map.of(
//                                                        "pkInvestmentScenarioId", Schema.builder().type("INTEGER").build(),
//                                                        "enumScenarioType", Schema.builder().type("STRING").build(),
//                                                        "decimprofitYield", Schema.builder().type("NUMBER").build(),
//                                                        "decimmonthlyCashflow", Schema.builder().type("NUMBER").build(),
//                                                        "decimprobability", Schema.builder().type("NUMBER").build(),
//                                                        "textMarketNote", Schema.builder().type("STRING").build(),
//                                                        "durationMonths", Schema.builder().type("INTEGER").build(),
//                                                        "decimpriceGrowthMin", Schema.builder().type("NUMBER").build(),
//                                                        "decimpriceGrowthMax", Schema.builder().type("NUMBER").build()
//                                                )).build())
//                                        .build(),
//                                "executionPlan", Schema.builder()
//                                        .type("OBJECT")
//                                        .properties(Map.ofEntries(
//                                                Map.entry("pkExecutionPlanId", Schema.builder().type("INTEGER").build()),
//                                                Map.entry("totalInvestmentCapital", Schema.builder().type("NUMBER").build()),
//                                                Map.entry("decimloanPercentage", Schema.builder().type("NUMBER").build()),
//                                                Map.entry("decimmonthlyPayment", Schema.builder().type("NUMBER").build()),
//                                                Map.entry("decimprobability", Schema.builder().type("NUMBER").build()),
//                                                Map.entry("decimnetCashflow", Schema.builder().type("NUMBER").build()),
//                                                Map.entry("maxHoldingMonths", Schema.builder().type("INTEGER").build()),
//                                                Map.entry("booleanIsLegalClear", Schema.builder().type("BOOLEAN").build()),
//                                                Map.entry("booleanIsLeverageSafe", Schema.builder().type("BOOLEAN").build()),
//                                                Map.entry("stringLiquidityDurationRange", Schema.builder().type("STRING").build()),
//                                                Map.entry("booleanIsReserveFundEnough", Schema.builder().type("BOOLEAN").build()),
//                                                Map.entry("textTakeProfitStrategy", Schema.builder().type("STRING").build()),
//                                                Map.entry("textHoldingTimeLimit", Schema.builder().type("STRING").build()),
//                                                Map.entry("textQuickSellAction", Schema.builder().type("STRING").build())
//                                        )).build()
//                        ))
//                        .required(List.of("score", "scenarios", "executionPlan"))
//                        .build())
//                .build();
//
//        int retryCount = 0;
//        GenerateContentResponse responseStage3 = null;
//
//        while (retryCount < 5) {
//            try {
//                responseStage3 = geminiClient.models.generateContent("gemini-2.5-flash", promptStage3, configStage3);
//                break;
//            } catch (Exception e) {
//                String errorMsg = e.getMessage() != null ? e.getMessage() : "";
//                // Nhận diện lỗi nghẽn mạch API hoặc lỗi dịch vụ gián đoạn từ phía đối tác để kích hoạt chờ đợi
//                if (errorMsg.contains("429") || errorMsg.contains("503") || errorMsg.contains("Unavailable") || errorMsg.contains("Quota exceeded") || errorMsg.contains("rate-limits")) {
//                    retryCount++;
//                    try {
//                        Thread.sleep(8000);
//                    } catch (InterruptedException ie) {
//                        Thread.currentThread().interrupt();
//                        throw new RuntimeException(ie);
//                    }
//                } else {
//                    throw e;
//                }
//            }
//        }
//
//        if (responseStage3 == null) {
//            throw new RuntimeException("Gemini API đang bận hoặc quá hạn mức (429/503). Vui lòng thử lại sau vài giây.");
//        }
//
//        try {
//            InvestmentPlanDTO finalInvestmentPlan = objectMapper.readValue(responseStage3.text().trim(), InvestmentPlanDTO.class);
//            finalInvestmentPlan.setInvestmentPortfolios(portfolios);
//            return finalInvestmentPlan;
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//
//    private void saveInvestmentPlanToDatabase(InvestmentPlanRequest request, InvestmentPlanDTO output, Strategy strategy) throws Exception {
//        LocalDateTime now = LocalDateTime.now();
//
//        Investor dbInvestor = null;
//        Account currentAccount = authenUntil.getCurrentUSer();
//        if (currentAccount != null && currentAccount.getInvestor() != null) {
//            dbInvestor = investorRepository.findById(currentAccount.getInvestor().getInvestorId()).orElse(null);
//        }
//
//        if (request.getStrategy_id() == null) {
//            throw new IllegalArgumentException("Strategy ID must not be null");
//        }
//
//        Map<String, Object> strategyDetailMap = request.getInvestmentStrategyDetail();
//
//        if (strategyDetailMap == null) {
//            strategyDetailMap = new HashMap<>();
//        }
//
//        String legalStatusJson = null;
//        if (request.getLegalStatus() != null && !request.getLegalStatus().isEmpty()) {
//            legalStatusJson = objectMapper.writeValueAsString(request.getLegalStatus());
//        }
//
//        InvestmentProfile profile = InvestmentProfile.builder()
//                .investor(dbInvestor)
//                .name("Kế hoạch"  +  " " + strategy.getName() + " " + request.getWardName())
//                .createdAt(now)
//                .isActive(true)
//                .updatedAt(now)
//                .profileVersions(new ArrayList<>())
//                .build();
//
//        InvestmentProfile savedProfile = investmentProfileRepository.save(profile);
//        int currentVersionsCount = 0;
//        int nextVersionNumber = currentVersionsCount + 1;
//        String autoVersionName = "Kế hoạch"  +  " " + strategy.getName() + " " + request.getWardName() + " - Version " + nextVersionNumber;
//        String autoVersionCode = "V" + nextVersionNumber;
//
//        InvestmentProfileVersion versionEntity = InvestmentProfileVersion.builder()
//                .investmentProfile(savedProfile)
//                .profileVersionName(autoVersionName)
//                .strategy(strategy)
//                .equity(request.getEquity())
//                .loanCapital(request.getLoanCapital())
//                .reserveFund(request.getReserveFund())
//                .conscious(request.getConsciousName())
//                .ward(request.getWardName())
//                .expectedRoi(request.getExpectedRoi())
//                .minProfit(null)
//                .riskToleranceLevel(request.getRiskToleranceLevel())
//                .durationYear(request.getDurationYear())
//                .startDate(request.getStartDate())
//                .investmentType(null)
//                .investmentStrategyDetail(strategyDetailMap)
//                .legalStatus(legalStatusJson)
//                .isActive(true)
//                .createdAt(now)
//                .updatedAt(now)
//                .investmentCriterias(new ArrayList<>())
//                .investmentScenarios(new ArrayList<>())
//                .investmentPortfolios(new ArrayList<>())
//                .executionPlans(new ArrayList<>())
//                .build();
//
//        InvestmentProfileVersion savedVersion = investmentProfileVersionRepository.save(versionEntity);
//
//        if (request.getCriteriaList() != null && !request.getCriteriaList().isEmpty()) {
//            for (CriteriaRequest critRequest : request.getCriteriaList()) {
//                if (critRequest.getPropertyTypeId() != null || critRequest.getPropertyConditionId() != null) {
//                    PropertyType pType = null;
//                    if (critRequest.getPropertyTypeId() != null) {
//                        pType = propertyTypeRepository.findById(critRequest.getPropertyTypeId()).orElse(null);
//                    }
//
//                    PropertyCondition pCondition = null;
//                    if (critRequest.getPropertyConditionId() != null) {
//                        pCondition = propertyConditionRepository.findById(critRequest.getPropertyConditionId()).orElse(null);
//                    }
//
//                    InvestmentCriteria criteriaEntity = InvestmentCriteria.builder()
//                            .investmentProfileVersion(savedVersion)
//                            .propertyType(pType)
//                            .propertyCondition(pCondition)
//                            .build();
//                    investmentCriteriaRepository.save(criteriaEntity);
//                }
//            }
//        }
//
//        if (output != null && output.getScenarios() != null) {
//            for (var scenarioDTO : output.getScenarios()) {
//                InvestmentScenario scenarioEntity = InvestmentScenario.builder()
//                        .investmentProfileVersion(savedVersion)
////                        .name(scenarioDTO.getEnumScenarioType())
//                        .scenarioType(scenarioDTO.getEnumScenarioType())
//                        .expectedReturnRate(scenarioDTO.getDecimprofitYield())
////                        .description(scenarioDTO.getTextMarketNote())
//                        .isActive(true)
//                        .createdAt(now)
//                        .updatedAt(now)
//                        .enumScenarioType(scenarioDTO.getEnumScenarioType())
//                        .decimprofitYield(scenarioDTO.getDecimprofitYield())
//                        .decimmonthlyCashflow(scenarioDTO.getDecimmonthlyCashflow())
//                        .decimprobability(scenarioDTO.getDecimprobability())
//                        .textMarketNote(scenarioDTO.getTextMarketNote())
//                        .durationMonths(scenarioDTO.getDurationMonths())
//                        .decimpriceGrowthMin(scenarioDTO.getDecimpriceGrowthMin())
//                        .decimpriceGrowthMax(scenarioDTO.getDecimpriceGrowthMax())
//                        .build();
//
//                investmentScenarioRepository.save(scenarioEntity);
//            }
//        }
//
//        if (output != null && output.getExecutionPlan() != null) {
//            var planDTO = output.getExecutionPlan();
//            String descJson = objectMapper.writeValueAsString(planDTO);
//            int scoreEvaluated = (output.getScore() != null) ? output.getScore() : 0;
//
//            ExecutionPlan planEntity = ExecutionPlan.builder()
//                    .investmentProfileVersion(savedVersion)
////                    .name("AI Execution Plan Details")
////                    .description(descJson)
//                    .match_score(scoreEvaluated)
////                    .status("ACTIVE")
//                    .createdAt(now)
//                    .updatedAt(now)
//                    .totalInvestmentCapital(planDTO.getTotalInvestmentCapital())
//                    .decimloanPercentage(planDTO.getDecimloanPercentage())
//                    .decimmonthlyPayment(planDTO.getDecimmonthlyPayment())
//                    .decimprobability(planDTO.getDecimprobability())
//                    .decimnetCashflow(planDTO.getDecimnetCashflow())
//                    .maxHoldingMonths(planDTO.getMaxHoldingMonths())
//                    .booleanIsLegalClear(planDTO.getBooleanIsLegalClear())
//                    .booleanIsLeverageSafe(planDTO.getBooleanIsLeverageSafe())
//                    .stringLiquidityDurationRange(planDTO.getStringLiquidityDurationRange())
//                    .booleanIsReserveFundEnough(planDTO.getBooleanIsReserveFundEnough())
//                    .textTakeProfitStrategy(planDTO.getTextTakeProfitStrategy())
//                    .textHoldingTimeLimit(planDTO.getTextHoldingTimeLimit())
//                    .textQuickSellAction(planDTO.getTextQuickSellAction())
//                    .build();
//
//            executionPlanRepository.save(planEntity);
//        }
//
//        if (output != null && output.getInvestmentPortfolios() != null) {
//            List<Portfolio> allowedPortfolios = new ArrayList<>();
//            if (strategy.getStrategyPortfolios() != null) {
//                for (StrategyPortfolio sp : strategy.getStrategyPortfolios()) {
//                    if (sp.getPortfolio() != null) {
//                        allowedPortfolios.add(sp.getPortfolio());
//                    }
//                }
//            }
//
//            if (allowedPortfolios.isEmpty()) {
//                allowedPortfolios = portfolioRepository.findAll();
//            }
//
//            for (var portDTO : output.getInvestmentPortfolios()) {
//                if (portDTO.getPortfolioName() == null) {
//                    continue;
//                }
//
//                final String targetName = portDTO.getPortfolioName().trim();
//                Portfolio dbPortfolio = allowedPortfolios.stream()
//                        .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(targetName))
//                        .findFirst()
//                        .orElse(null);
//
//                if (dbPortfolio == null) {
//                    continue;
//                }
//
//                InvestmentPortfolio portEntity = InvestmentPortfolio.builder()
//                        .investmentProfileVersion(savedVersion)
//                        .portfolio(dbPortfolio)
//                        .percentage(portDTO.getPercentage())
//                        .capital(portDTO.getCapital())
//                        .isActive(true)
//                        .createdAt(now)
//                        .updatedAt(now)
//                        .build();
//
//                InvestmentPortfolio savedPortEntity = investmentPortfolioRepository.save(portEntity);
//
//                if (portDTO.getAllocations() != null) {
//                    for (var allocDTO : portDTO.getAllocations()) {
//                        PortfolioAllocation allocEntity = PortfolioAllocation.builder()
//                                .portfolio(dbPortfolio)
//                                .investmentPortfolio(savedPortEntity)
//                                .isActive(true)
//                                .createdAt(now)
//                                .updatedAt(now)
//                                .build();
//
//                        PortfolioAllocation savedAllocEntity = portfolioAllocationRepository.save(allocEntity);
//
//                        if (allocDTO.getProperties() != null) {
//                            for (var propDTO : allocDTO.getProperties()) {
//                                if (propDTO.getPortfolioAllocationPropertyId() != null) {
//                                    Listing listing = listingRepository.findById(propDTO.getPortfolioAllocationPropertyId()).orElse(null);
//                                    Property propertyRelation = (listing != null) ? listing.getProperty() : null;
//
//                                    PortfolioAllocationProperty propEntity = PortfolioAllocationProperty.builder()
//                                            .portfolioAllocation(savedAllocEntity)
//                                            .property(propertyRelation)
//                                            .weight(1.0)
//                                            .isActive(true)
//                                            .createdAt(now)
//                                            .updatedAt(now)
//                                            .build();
//                                    portfolioAllocationPropertyRepository.save(propEntity);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    @Override
//    @Transactional
//    public ResponseEntity<ApiResponse> updateExistingInvestmentPlan(Integer currentProfileId, UpdateInvestmentPlanRequest request) {
//        try {
//            InvestmentProfile oldProfile = investmentProfileRepository.findById(currentProfileId).orElse(null);
//            if (oldProfile == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("Profile_Not_Found", "The investment profile you want to update does not exist."));
//            }
//
//            Account account = authenUntil.getCurrentUSer();
//            if (account == null) {
//                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                        .body(ApiResponse.fail(HttpStatus.NOT_FOUND.toString(), "Account does not exist."));
//            }
//
//            if (account.getWallet() == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("WALLET_NOT_FOUND", "You don't have a wallet. Please deposit money into your wallet to use this feature."));
//            }
//
//            Investor investor = account.getInvestor();
//            if (investor == null || investor.getMembershipSubscriptions() == null || investor.getMembershipSubscriptions().isEmpty()) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("MEMBERSHIP_NOT_FOUND", "You don't have any membership subscription. Please purchase a plan to use this feature."));
//            }
//
//            MembershipSubscription activeSubscription = investor.getMembershipSubscriptions().stream()
//                    .filter(sub -> sub.getMembershipSubscriptionEnum_status() != null
//                            && sub.getMembershipSubscriptionEnum_status().equals(MembershipSubscriptionEnum.Using)
//                            && Boolean.TRUE.equals(sub.getIsActive()))
//                    .findFirst()
//                    .orElse(null);
//
//            if (activeSubscription == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("SUBSCRIPTION_NOT_ACTIVATED", "You have purchased a membership subscription, but it is not activated yet. Please activate your subscription to use this feature."));
//            }
//
//            if (activeSubscription.getQuantity_using() == null || activeSubscription.getQuantity_using() <= 0) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("QUANTITY_EXHAUSTED", "Your membership subscription has run out of usage limit. Please renew or purchase a new plan."));
//            }
//
//            Strategy strategy = strategyRepository.findById(request.getStrategy_id()).orElse(null);
//            if (strategy == null) {
//                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                        .body(ApiResponse.fail("Strategy_Not_Found", "Investment strategy not found."));
//            }
//
//            InvestmentPlanRequest internalRequest = new InvestmentPlanRequest();
//            internalRequest.setStrategy_id(request.getStrategy_id());
//            internalRequest.setName(oldProfile.getName());
//            internalRequest.setEquity(request.getEquity());
//            internalRequest.setLoanCapital(request.getLoanCapital());
//            internalRequest.setReserveFund(request.getReserveFund());
//            internalRequest.setConsciousName(request.getConsciousName());
//            internalRequest.setWardName(request.getWardName());
//            internalRequest.setExpectedRoi(request.getExpectedRoi());
//            internalRequest.setRiskToleranceLevel(request.getRiskToleranceLevel());
//            internalRequest.setDurationYear(request.getDurationYear());
//            internalRequest.setStartDate(request.getStartDate());
//            internalRequest.setInvestmentStrategyDetail(request.getInvestmentStrategyDetail());
//            internalRequest.setLegalStatus(request.getLegalStatus());
//            internalRequest.setCriteriaList(request.getCriteriaList());
//
//            List<InvestmentPortfolioDTO> portfolios = processStage1Portfolios(internalRequest, strategy);
//            processStage2EnrichProperties(internalRequest, portfolios);
//            InvestmentPlanDTO finalOutput = processStage3ScenariosAndExecution(internalRequest, portfolios, oldProfile.getInvestor());
//            saveUpdatePlanToDatabase(oldProfile, internalRequest, finalOutput, strategy);
//
//            int remainingQuantity = activeSubscription.getQuantity_using() - 1;
//            activeSubscription.setQuantity_using(remainingQuantity);
//
//            if (remainingQuantity <= 0) {
//                activeSubscription.setIsActive(false);
//                activeSubscription.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.OutDated);
//            }
//
//            activeSubscription.setUpdatedAt(LocalDateTime.now());
//            membershipSubscriptionRepository.save(activeSubscription);
//
//            return ResponseEntity.status(HttpStatus.OK)
//                    .body(ApiResponse.success(finalOutput, "Updated and saved new version of investment plan successfully"));
//        } catch (Exception e) {
//            log.error("Error in updateExistingInvestmentPlan", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
//        }
//    }
//
//    private void saveUpdatePlanToDatabase(InvestmentProfile oldProfile, InvestmentPlanRequest request, InvestmentPlanDTO output, Strategy strategy) throws Exception {
//        LocalDateTime now = LocalDateTime.now();
//
//
//        oldProfile.setUpdatedAt(now);
//        InvestmentProfile savedProfile = investmentProfileRepository.save(oldProfile);
//
//        if (savedProfile.getProfileVersions() != null) {
//            for (InvestmentProfileVersion oldVersion : savedProfile.getProfileVersions()) {
//                if (Boolean.TRUE.equals(oldVersion.getIsActive())) {
//                    oldVersion.setIsActive(false);
//                    oldVersion.setUpdatedAt(now);
//                    investmentProfileVersionRepository.save(oldVersion);
//                }
//            }
//        }
//
//        int currentVersionsCount = (savedProfile.getProfileVersions() != null) ? savedProfile.getProfileVersions().size() : 0;
//        int nextVersionNumber = currentVersionsCount + 1;
//        String autoVersionName = strategy.getName() + " " + request.getWardName() + " - Version " + nextVersionNumber;
//        String autoVersionCode = "V" + nextVersionNumber;
//
//        final Integer currentInvestorId = oldProfile.getInvestor() != null ? oldProfile.getInvestor().getInvestorId() : null;
//        final String targetName = oldProfile.getName();
//
//
//        String legalStatusJson = null;
//        if (request.getLegalStatus() != null && !request.getLegalStatus().isEmpty()) {
//            legalStatusJson = objectMapper.writeValueAsString(request.getLegalStatus());
//        }
//
//        Map<String, Object> strategyDetailMap = request.getInvestmentStrategyDetail();
//        if (strategyDetailMap == null) {
//            strategyDetailMap = new HashMap<>();
//        }
//
//        InvestmentProfileVersion versionEntity = InvestmentProfileVersion.builder()
//                .investmentProfile(savedProfile)
//                .profileVersionName(autoVersionName)
//                .strategy(strategy)
//                .equity(request.getEquity())
//                .loanCapital(request.getLoanCapital())
//                .reserveFund(request.getReserveFund())
//                .conscious(request.getConsciousName())
//                .ward(request.getWardName())
//                .expectedRoi(request.getExpectedRoi())
////                .minProfit(request.getMinProfit())
//                .riskToleranceLevel(request.getRiskToleranceLevel())
//                .durationYear(request.getDurationYear())
//                .startDate(request.getStartDate())
//                .legalStatus(legalStatusJson)
////                .investmentType(request.getInvestmentType())
//                .investmentStrategyDetail(strategyDetailMap)
//                .isActive(true)
//                .createdAt(now)
//                .updatedAt(now)
//                .investmentCriterias(new ArrayList<>())
//                .investmentScenarios(new ArrayList<>())
//                .investmentPortfolios(new ArrayList<>())
//                .executionPlans(new ArrayList<>())
//                .build();
//
//        InvestmentProfileVersion savedVersion = investmentProfileVersionRepository.save(versionEntity);
//        if (request.getCriteriaList() != null && !request.getCriteriaList().isEmpty()) {
//            for (CriteriaRequest critRequest : request.getCriteriaList()) {
//                if (critRequest.getPropertyTypeId() != null || critRequest.getPropertyConditionId() != null) {
//                    PropertyType pType = null;
//                    if (critRequest.getPropertyTypeId() != null) {
//                        pType = propertyTypeRepository.findById(critRequest.getPropertyTypeId()).orElse(null);
//                    }
//
//                    PropertyCondition pCondition = null;
//                    if (critRequest.getPropertyConditionId() != null) {
//                        pCondition = propertyConditionRepository.findById(critRequest.getPropertyConditionId()).orElse(null);
//                    }
//
//                    InvestmentCriteria criteriaEntity = InvestmentCriteria.builder()
//                            .investmentProfileVersion(savedVersion)
//                            .propertyType(pType)
//                            .propertyCondition(pCondition)
//                            .build();
//                    investmentCriteriaRepository.save(criteriaEntity);
//                }
//            }
//        }
//
//        if (output != null && output.getScenarios() != null) {
//            for (var scenarioDTO : output.getScenarios()) {
//                InvestmentScenario scenarioEntity = InvestmentScenario.builder()
//                        .investmentProfileVersion(savedVersion)
////                        .name(scenarioDTO.getEnumScenarioType())
//                        .scenarioType(scenarioDTO.getEnumScenarioType())
//                        .expectedReturnRate(scenarioDTO.getDecimprofitYield())
////                        .description(scenarioDTO.getTextMarketNote())
//                        .isActive(true)
//                        .createdAt(now)
//                        .updatedAt(now)
//                        .build();
//
//                investmentScenarioRepository.save(scenarioEntity);
//            }
//        }
//
//        if (output != null && output.getExecutionPlan() != null) {
//            var planDTO = output.getExecutionPlan();
//            String descJson = objectMapper.writeValueAsString(planDTO);
//
//            int scoreEvaluated = (output.getScore() != null) ? output.getScore() : 0;
//
//            ExecutionPlan planEntity = ExecutionPlan.builder()
//                    .investmentProfileVersion(savedVersion)
////                    .name("AI Execution Plan Details")
////                    .description(descJson)
//                    .match_score(scoreEvaluated)
////                    .status("ACTIVE")
//                    .createdAt(now)
//                    .updatedAt(now)
//                    .build();
//
//            executionPlanRepository.save(planEntity);
//        }
//
//        if (output != null && output.getInvestmentPortfolios() != null) {
//            List<Portfolio> allowedPortfolios = new ArrayList<>();
//            if (strategy.getStrategyPortfolios() != null) {
//                for (StrategyPortfolio sp : strategy.getStrategyPortfolios()) {
//                    if (sp.getPortfolio() != null) {
//                        allowedPortfolios.add(sp.getPortfolio());
//                    }
//                }
//            }
//
//            if (allowedPortfolios.isEmpty()) {
//                allowedPortfolios = portfolioRepository.findAll();
//            }
//
//            for (var portDTO : output.getInvestmentPortfolios()) {
//                if (portDTO.getPortfolioName() == null) {
//                    continue;
//                }
//
//                final String targetNamePort = portDTO.getPortfolioName().trim();
//                Portfolio dbPortfolio = allowedPortfolios.stream()
//                        .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(targetNamePort))
//                        .findFirst()
//                        .orElse(null);
//
//                if (dbPortfolio == null) {
//                    continue;
//                }
//
//                InvestmentPortfolio portEntity = InvestmentPortfolio.builder()
//                        .investmentProfileVersion(savedVersion)
//                        .portfolio(dbPortfolio)
//                        .percentage(portDTO.getPercentage())
//                        .capital(portDTO.getCapital())
//                        .isActive(true)
//                        .createdAt(now)
//                        .updatedAt(now)
//                        .build();
//
//                InvestmentPortfolio savedPortEntity = investmentPortfolioRepository.save(portEntity);
//
//                if (portDTO.getAllocations() != null) {
//                    for (var allocDTO : portDTO.getAllocations()) {
//                        PortfolioAllocation allocEntity = PortfolioAllocation.builder()
//                                .portfolio(dbPortfolio)
//                                .investmentPortfolio(savedPortEntity)
//                                .isActive(true)
//                                .createdAt(now)
//                                .updatedAt(now)
//                                .build();
//
//                        PortfolioAllocation savedAllocEntity = portfolioAllocationRepository.save(allocEntity);
//
//                        if (allocDTO.getProperties() != null) {
//                            for (var propDTO : allocDTO.getProperties()) {
//                                if (propDTO.getPortfolioAllocationPropertyId() != null) {
//                                    Listing listing = listingRepository.findById(propDTO.getPortfolioAllocationPropertyId()).orElse(null);
//                                    Property propertyRelation = (listing != null) ? listing.getProperty() : null;
//
//                                    PortfolioAllocationProperty propEntity = PortfolioAllocationProperty.builder()
//                                            .portfolioAllocation(savedAllocEntity)
//                                            .property(propertyRelation)
//                                            .weight(1.0)
//                                            .isActive(true)
//                                            .createdAt(now)
//                                            .updatedAt(now)
//                                            .build();
//                                    portfolioAllocationPropertyRepository.save(propEntity);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }


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

    @Override
    public ResponseEntity<ApiResponse> deleteInvestmentPlan(Integer profileId) {
        try {
            InvestmentProfile investmentProfile = investmentProfileRepository.findById(profileId).orElse(null);
            if (investmentProfile == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.success(null, "Id does not exist"));
            }
            for (InvestmentProfileVersion investmentProfileVersion : investmentProfile.getProfileVersions()) {
                investmentProfileVersion.setIsActive(false);
                investmentProfileVersionRepository.save(investmentProfileVersion);
            }
            investmentProfile.setIsActive(false);
            investmentProfileRepository.save(investmentProfile);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Deleted investment plan and all its versions successfully"));
        } catch (Exception e) {
            log.error("Error in deleteInvestmentPlan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateProfileName(Integer profileId, String newName) {
        try {
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Invalid_Name", "Name must not be empty"));
            }

            InvestmentProfile investmentProfile = investmentProfileRepository.findById(profileId).orElse(null);
            if (investmentProfile == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Profile_Not_Found", "Investment profile does not exist"));
            }

            investmentProfile.setName(newName.trim());
            investmentProfile.setUpdatedAt(LocalDateTime.now());
            investmentProfileRepository.save(investmentProfile);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Updated investment profile name successfully"));

        } catch (Exception e) {
            log.error("Error in updateProfileName", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> updateVersionName(Integer versionId, String newName) {
        try {
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Invalid_Name", "Version name must not be empty"));
            }

            InvestmentProfileVersion profileVersion = investmentProfileVersionRepository.findById(versionId).orElse(null);
            if (profileVersion == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Version_Not_Found", "Investment profile version does not exist"));
            }

            profileVersion.setProfileVersionName(newName.trim());
            profileVersion.setUpdatedAt(LocalDateTime.now());
            investmentProfileVersionRepository.save(profileVersion);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(null, "Updated profile version name successfully"));

        } catch (Exception e) {
            log.error("Error in updateVersionName", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

}