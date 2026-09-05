package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.MembershipSubscriptionEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.*;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestmentPlanServiceInterface;
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

    private static final double OPTIMISTIC_INTEREST_RATE = 0.08;
    private static final double PESSIMISTIC_INTEREST_RATE = 0.12;
    private static final double TRANSACTION_COST_RATE = 0.035;
    private static final double CAPEX_RATE = 0.05;
    private static final double GAIN_RATE_OPTIMISTIC = 0.15;
    private static final double SAVING_INTEREST_RATE_PER_YEAR = 0.06;
    private static final int LOAN_MONTHS = 240;
    private static final int HOLDING_MONTHS_LUOT_SONG = 3;
    private static final int HOLDING_MONTHS_MUA_SUA_BAN = 9;
    private static final int HOLDING_MONTHS_DONG_TIEN = 60;

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
    private InvestmentProfileRepository investmentProfileRepository;

    @Autowired
    private InvestmentCriteriaRepository investmentCriteriaRepository;

    @Autowired
    private AuthenUntil authenUntil;

    @Autowired
    private InvestorRepository investorRepository;

    @Autowired
    private InvestmentProfileVersionRepository investmentProfileVersionRepository;

    @Autowired
    private PropertyScenarioRepository propertyScenarioRepository;

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
                if (profile.getProfileVersions() == null) continue;

                InvestmentProfileVersion latestVersion = profile.getProfileVersions().stream()
                        .filter(v -> v.getCreatedAt() != null)
                        .filter(v -> Boolean.TRUE.equals(v.getIsActive()))
                        .max(Comparator.comparing(InvestmentProfileVersion::getCreatedAt))
                        .orElse(null);

                if (latestVersion == null) continue;

                simpleProfiles.add(ProfileSimpleDTO.builder()
                        .investmentProfileId(profile.getInvestmentProfileId())
                        .latestVersionId(latestVersion.getProfileVersionId())
                        .totalCapital(latestVersion.getTotalCapital() != null ? latestVersion.getTotalCapital() : 0)
                        .name(profile.getName())
                        .longTermYear(latestVersion.getLongTermYear())
                        .consciousName(latestVersion.getConscious())
                        .wardName(latestVersion.getWards() != null ? latestVersion.getWards() : new ArrayList<>())
                        .isActive(latestVersion.getIsActive())
                        .equity(latestVersion.getEquity())
                        .strategyName(latestVersion.getStrategy() != null ? latestVersion.getStrategy().getName() : "N/A")
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
                    .map(version -> ProfileVersionDTO.builder()
                            .investmentProfileVersionId(version.getProfileVersionId())
                            .totalCapital(version.getTotalCapital())
                            .name(version.getProfileVersionName())
                            .longTermYear(version.getLongTermYear())
                            .consciousName(version.getConscious())
                            .wardName(version.getWards() != null ? version.getWards() : new ArrayList<>())
                            .isActive(version.getIsActive())
                            .equity(version.getEquity() != null ? version.getEquity() : 0L)
                            .strategyName(version.getStrategy() != null ? version.getStrategy().getName() : "N/A")
                            .createdAt(version.getCreatedAt())
                            .build())
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
                        .body(ApiResponse.fail("Profile_Not_Found", "Investment profile version không tồn tại với ID: " + profileVersionId));
            }

            InvestmentProfile profile = profileVersion.getInvestmentProfile();
            if (profile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Profile_Not_Found", "Không tìm thấy thông tin kế hoạch cha gốc của phiên bản này."));
            }

            List<InvestmentCriteriaDTO> criteriaDTOList = Collections.emptyList();
            if (profileVersion.getInvestmentCriterias() != null) {
                criteriaDTOList = profileVersion.getInvestmentCriterias().stream()
                        .map(criteria -> InvestmentCriteriaDTO.builder()
                                .propertyTypeName(criteria.getPropertyType() != null ? criteria.getPropertyType().getName() : null)
                                .build())
                        .collect(Collectors.toList());
            }

            InvestmentProfileVersionDTO profileDTO = InvestmentProfileVersionDTO.builder()
                    .investmentProfileVersionId(profileVersion.getProfileVersionId())
                    .strategyName(profileVersion.getStrategy() != null ? profileVersion.getStrategy().getName() : null)
                    .name(profileVersion.getProfileVersionName())
                    .equity(profileVersion.getEquity())
                    .loanCapital(profileVersion.getLoanCapital())
                    .longTermYear(profileVersion.getLongTermYear())
                    .currentCashflow(profileVersion.getCurrentCashflow())
                    .consciousName(profileVersion.getConscious())
                    .wardName(profileVersion.getWards() != null ? profileVersion.getWards() : new ArrayList<>())
                    .investmentStrategyDetail(profileVersion.getInvestmentStrategyDetail())
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

            long totalCapital = (profileVersion.getEquity() != null ? profileVersion.getEquity() : 0L)
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
                                                                .monthlyRentalCashflow(prop.getMonthlyRentalCashflow() != null && prop.getMonthlyRentalCashflow() != 0 ? prop.getMonthlyRentalCashflow() : null)
                                                                .monthlyPrincipalInterest(prop.getMonthlyPrincipalInterest())
                                                                .netCashflow(prop.getNetCashflow() != null && prop.getNetCashflow() != 0 ? prop.getNetCashflow() : null)
                                                                .roiPercentage(prop.getRoiPercentage())
                                                                .build())
                                                        .scenarios(prop.getScenarios() != null ? prop.getScenarios().stream()
                                                                .map(s -> PropertyScenarioDTO.builder()
                                                                        .scenarioType(s.getScenarioType())
                                                                        .interestRate(s.getInterestRate())
                                                                        .occupancyRate(s.getOccupancyRate())
                                                                        .monthlyPayment(s.getMonthlyPayment())
                                                                        .monthlyCashflowIn(s.getMonthlyCashflowIn())
                                                                        .netCashflow(s.getNetCashflow() != null && s.getNetCashflow() != 0 ? s.getNetCashflow() : null)
                                                                        .survivalCashflow(s.getSurvivalCashflow())
                                                                        .totalNetProfit(s.getTotalNetProfit())
                                                                        .roiPercentage(s.getRoiPercentage())
                                                                        .riskLabel(s.getRiskLabel())
                                                                        .isWorthInvesting(s.getIsWorthInvesting())
                                                                        .build())
                                                                .collect(Collectors.toList()) : new ArrayList<>())
                                                        .build())
                                                .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList());
            }

            InvestmentProfile profile = profileVersion.getInvestmentProfile();

            InvestmentPlanDTO finalOutput = InvestmentPlanDTO.builder()
                    .investmentProfileId(profile != null ? profile.getInvestmentProfileId() : null)
                    .investmentProfileVersionId(profileVersion.getProfileVersionId())
                    .totalCapital(totalCapital)
                    .loanCapital(profileVersion.getLoanCapital())
                    .equity(profileVersion.getEquity())
                    .strategyName(profileVersion.getStrategy().getName())
                    .investmentCriteriaDTOV2s(criteriaDTOV2s)
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

            if (investor.getMembershipSubscriptions() == null || investor.getMembershipSubscriptions().isEmpty()) {
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
                        .body(ApiResponse.fail("SUBSCRIPTION_NOT_ACTIVATED", "You have purchased a membership subscription, but it is not activated yet. Please activate your subscription to use this feature."));
            }

            if (activeSubscription.getQuantity_using() == null || activeSubscription.getQuantity_using() <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("QUANTITY_EXHAUSTED", "Your membership subscription has run out of usage limit. Please renew or purchase a new plan."));
            }

            InvestmentPlanDTO finalOutput = buildInvestmentPlan(request, strategy);

            InvestmentProfileVersion savedVersion = saveNewInvestmentPlan(investor, request, finalOutput, strategy);

            if (savedVersion != null && savedVersion.getInvestmentProfile() != null) {
                finalOutput.setInvestmentProfileId(savedVersion.getInvestmentProfile().getInvestmentProfileId());
                finalOutput.setInvestmentProfileVersionId(savedVersion.getProfileVersionId());
            }

            int remainingQuantity = activeSubscription.getQuantity_using() - 1;
            activeSubscription.setQuantity_using(remainingQuantity);
            if (remainingQuantity <= 0) {
                activeSubscription.setIsActive(false);
                activeSubscription.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.OutDated);
            }
            activeSubscription.setUpdatedAt(LocalDateTime.now());
            membershipSubscriptionRepository.save(activeSubscription);

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
    public ResponseEntity<ApiResponse> updateExistingInvestmentPlan(Integer currentProfileId, UpdateInvestmentPlanRequest request) {
        try {
            InvestmentProfile existingProfile = investmentProfileRepository.findById(currentProfileId).orElse(null);
            if (existingProfile == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Profile_Not_Found", "Investment profile not found."));
            }

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

            if (investor.getMembershipSubscriptions() == null || investor.getMembershipSubscriptions().isEmpty()) {
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
                        .body(ApiResponse.fail("SUBSCRIPTION_NOT_ACTIVATED", "You have purchased a membership subscription, but it is not activated yet. Please activate your subscription to use this feature."));
            }

            if (activeSubscription.getQuantity_using() == null || activeSubscription.getQuantity_using() <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("QUANTITY_EXHAUSTED", "Your membership subscription has run out of usage limit. Please renew or purchase a new plan."));
            }

            for (InvestmentProfileVersion version : existingProfile.getProfileVersions()) {
                version.setIsActive(false);
                investmentProfileVersionRepository.save(version);
            }

            InvestmentPlanRequest convertedRequest = InvestmentPlanRequest.builder()
                    .equity(request.getEquity())
                    .loanCapital(request.getLoanCapital())
                    .currentCashFlow(request.getCurrentCashFlow())
                    .consciousName(request.getConsciousName())
                    .wardNames(request.getWardNames())
                    .longTermYear(request.getLongTermYear())
                    .strategyId(request.getStrategyId())
                    .investmentStrategyDetail(request.getInvestmentStrategyDetail())
                    .criteriaList(request.getCriteriaList())
                    .build();

            InvestmentPlanDTO finalOutput = buildInvestmentPlan(convertedRequest, strategy);

            InvestmentProfileVersion savedVersion = saveUpdateInvestmentPlan(existingProfile, convertedRequest, finalOutput, strategy);

            if (savedVersion != null && savedVersion.getInvestmentProfile() != null) {
                finalOutput.setInvestmentProfileId(savedVersion.getInvestmentProfile().getInvestmentProfileId());
                finalOutput.setInvestmentProfileVersionId(savedVersion.getProfileVersionId());
            }

            int remainingQuantity = activeSubscription.getQuantity_using() - 1;
            activeSubscription.setQuantity_using(remainingQuantity);
            if (remainingQuantity <= 0) {
                activeSubscription.setIsActive(false);
                activeSubscription.setMembershipSubscriptionEnum_status(MembershipSubscriptionEnum.OutDated);
            }
            activeSubscription.setUpdatedAt(LocalDateTime.now());
            membershipSubscriptionRepository.save(activeSubscription);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(finalOutput, "Update investment plan version successfully"));

        } catch (Exception e) {
            log.error("Error in updateExistingInvestmentPlan", e);
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

            Strategy strategy = strategyRepository.findById(saveRequest.getInputRequest().getStrategyId()).orElse(null);
            if (strategy == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("Strategy_Not_Found", "Investment strategy not found."));
            }

            Account account = authenUntil.getCurrentUSer();
            Investor investor = account != null ? account.getInvestor() : null;
            if (investor == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.fail("INVESTOR_NOT_FOUND", "Investor profile not found."));
            }

            InvestmentPlanDTO outputData = saveRequest.getAiOutputData();
            InvestmentProfileVersion savedVersion = saveNewInvestmentPlan(investor, saveRequest.getInputRequest(), outputData, strategy);

            if (savedVersion != null && savedVersion.getInvestmentProfile() != null) {
                outputData.setInvestmentProfileId(savedVersion.getInvestmentProfile().getInvestmentProfileId());
                outputData.setInvestmentProfileVersionId(savedVersion.getProfileVersionId());
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(outputData, "Investment plan logged and saved successfully"));

        } catch (Exception e) {
            log.error("Error in savePlanToDatabaseDirectly", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private InvestmentPlanDTO buildInvestmentPlan(InvestmentPlanRequest request, Strategy strategy) throws Exception {
        long totalCapital = request.getEquity() + request.getLoanCapital();
        String strategyName = strategy.getName() != null ? strategy.getName() : "";

        String stNameLower = strategyName.toLowerCase();
        boolean isDongTien = stNameLower.contains("dòng tiền") || stNameLower.contains("dong tien");
        boolean isMuaSuaBan = stNameLower.contains("sửa") || stNameLower.contains("sua");
        boolean isLuotSong = stNameLower.contains("lướt") || stNameLower.contains("luot");

        int holdingMonths = getDefaultHoldingMonths(strategyName);

        int loanTermYears = request.getLongTermYear() > 0
                ? request.getLongTermYear()
                : 20;

        long additionalCostReq = 0L;

        if (request.getInvestmentStrategyDetail() != null
                && request.getInvestmentStrategyDetail().get("additionalCost") != null) {

            Object addCostObj =
                    request.getInvestmentStrategyDetail().get("additionalCost");

            if (addCostObj instanceof Number) {
                additionalCostReq =
                        ((Number) addCostObj).longValue();
            }
        }

        List<InvestmentCriteriaDTOV2> criteriaList =
                new ArrayList<>();

        List<Map<String, Object>> aiCriteria =
                new ArrayList<>();

        for (var crit : request.getCriteriaList()) {

            List<ProposedPropertyDTO> properties =
                    new ArrayList<>();

            List<Map<String, Object>> aiProperties =
                    new ArrayList<>();

            boolean hasLoan =
                    request.getLoanCapital() != null
                            && request.getLoanCapital() > 0;

            if (hasLoan) {

                List<Listing> totalCapitalListings =
                        listingRepository.findListingsByTotalCapitalRange(
                                crit.getPropertyTypeId(),
                                request.getWardNames(),
                                request.getEquity(),
                                totalCapital
                        );

                Listing firstListing =
                        !totalCapitalListings.isEmpty()
                                ? totalCapitalListings.get(0)
                                : null;

                Integer firstListingId =
                        firstListing != null
                                ? firstListing.getListingId()
                                : null;

                if (firstListing != null) {
                    addProperty(
                            firstListing,
                            "TOTAL_CAPITAL_BASED",
                            properties,
                            aiProperties
                    );
                }

                List<Listing> equityListings =
                        listingRepository.findListingsByEquityBudget(
                                crit.getPropertyTypeId(),
                                request.getWardNames(),
                                request.getEquity()
                        );

                Listing secondListing =
                        equityListings.stream()
                                .filter(l ->
                                        !Objects.equals(
                                                l.getListingId(),
                                                firstListingId
                                        )
                                )
                                .findFirst()
                                .orElse(null);

                if (secondListing != null) {
                    addProperty(
                            secondListing,
                            "EQUITY_BASED",
                            properties,
                            aiProperties
                    );
                }

            } else {

                List<Listing> equityListings =
                        listingRepository.findListingsByEquityBudget(
                                crit.getPropertyTypeId(),
                                request.getWardNames(),
                                request.getEquity()
                        );

                if (!equityListings.isEmpty()) {
                    addProperty(
                            equityListings.get(0),
                            "EQUITY_BASED",
                            properties,
                            aiProperties
                    );
                }
            }

            criteriaList.add(
                    InvestmentCriteriaDTOV2.builder()
                            .proposedPropertyDTOList(properties)
                            .build()
            );

            aiCriteria.add(
                    Map.of(
                            "propertyTypeId",
                            crit.getPropertyTypeId(),
                            "properties",
                            aiProperties
                    )
            );
        }

        String propertiesJson =
                objectMapper.writeValueAsString(aiCriteria);

        String prompt = """
        Bạn là chuyên gia phân tích tài chính và bất động sản tại TP.HCM.
        Hãy thực hiện tính toán tài chính chi tiết dựa trên các quy tắc chiến lược được quy định bên dưới và trả về đúng định dạng JSON theo schema đã cho.

        THÔNG TIN TÀI CHÍNH ĐẦU VÀO:
        - Vốn vay ngân hàng (loanCapital): %d VNĐ
        - Thời hạn vay (longTermYear): %d năm
        - Chi phí phát sinh (additionalCost): %d VNĐ
        - Chiến lược đang áp dụng: %s

        QUY TẮC CÔNG THỨC TRẢ GỐC LÃI HÀNG THÁNG (monthlyPrincipalInterest):
        - P = loanCapital. Nếu P <= 0 thì monthlyPrincipalInterest = 0.
        - Lãi suất Big4: r = 0.07 / 12, số tháng vay: n = longTermYear * 12.
        - Công thức: monthlyPrincipalInterest = P * [r * (1 + r)^n] / [(1 + r)^n - 1]

        QUY TẮC TÍNH TOÁN DÒNG TIỀN VÀ LỢI NHUẬN THEO CHIẾN LƯỢC:

        1. Chiến lược "Đầu Cơ Lướt Sóng" (strategy_id = 1):
           - monthlyRentalCashflow = 0
           - netCashflow = 0
           - estimatedProfit = valuePrice * (1 + 0.08)^0.25 - valuePrice

        2. Chiến lược "Mua Sửa Bán" (strategy_id = 2):
           - monthlyRentalCashflow = 0
           - netCashflow = 0
           - additionalCost: Lấy từ request (nếu = 0 thì mặc định valuePrice * 0.05)
           - estimatedProfit = [valuePrice * (1 + 0.20)^0.75 - valuePrice] - additionalCost

        3. Chiến lược "BĐS Dòng Tiền" (strategy_id = 3):
           - Yield cho thuê theo property_type_id:
             + property_type_id = 1: Yield = 0.028
             + property_type_id = 2: Yield = 0.0
             + property_type_id = 3: Yield = 0.050
             + property_type_id = 4: Yield = 0.075
             + property_type_id = 5: Yield = 0.040
             + property_type_id = 6: Yield = 0.060
             + Khác: Yield = 0.040
           - monthlyRentalCashflow = (valuePrice * Yield) / 12
           - netCashflow = monthlyRentalCashflow - monthlyPrincipalInterest
           - estimatedProfit = valuePrice * (1 + 0.07 + Yield)^5 - valuePrice

        DANH SÁCH BẤT ĐỘNG SẢN:
        %s
        """.formatted(
                request.getLoanCapital(),
                loanTermYears,
                additionalCostReq,
                strategyName,
                propertiesJson
        );

        Schema financialMetricsSchema =
                Schema.builder()
                        .type("OBJECT")
                        .properties(
                                Map.of(
                                        "estimatedProfit",
                                        Schema.builder()
                                                .type("NUMBER")
                                                .build(),

                                        "monthlyRentalCashflow",
                                        Schema.builder()
                                                .type("NUMBER")
                                                .build(),

                                        "monthlyPrincipalInterest",
                                        Schema.builder()
                                                .type("NUMBER")
                                                .build(),

                                        "netCashflow",
                                        Schema.builder()
                                                .type("NUMBER")
                                                .build(),

                                        "roiPercentage",
                                        Schema.builder()
                                                .type("NUMBER")
                                                .build()
                                )
                        )
                        .required(
                                List.of(
                                        "estimatedProfit",
                                        "monthlyRentalCashflow",
                                        "monthlyPrincipalInterest",
                                        "netCashflow",
                                        "roiPercentage"
                                )
                        )
                        .build();

        Schema propertySchema =
                Schema.builder()
                        .type("OBJECT")
                        .properties(
                                Map.of(
                                        "listingId",
                                        Schema.builder()
                                                .type("INTEGER")
                                                .build(),

                                        "financialMetrics",
                                        financialMetricsSchema
                                )
                        )
                        .required(
                                List.of(
                                        "listingId",
                                        "financialMetrics"
                                )
                        )
                        .build();

        Schema criteriaSchema =
                Schema.builder()
                        .type("OBJECT")
                        .properties(
                                Map.of(
                                        "proposedPropertyDTOList",
                                        Schema.builder()
                                                .type("ARRAY")
                                                .items(propertySchema)
                                                .build()
                                )
                        )
                        .required(
                                List.of(
                                        "proposedPropertyDTOList"
                                )
                        )
                        .build();

        Schema dataSchema =
                Schema.builder()
                        .type("OBJECT")
                        .properties(
                                Map.of(
                                        "investmentCriteriaDTOV2s",
                                        Schema.builder()
                                                .type("ARRAY")
                                                .items(criteriaSchema)
                                                .build()
                                )
                        )
                        .required(
                                List.of(
                                        "investmentCriteriaDTOV2s"
                                )
                        )
                        .build();

        Schema rootSchema =
                Schema.builder()
                        .type("OBJECT")
                        .properties(
                                Map.of(
                                        "data",
                                        dataSchema
                                )
                        )
                        .required(
                                List.of("data")
                        )
                        .build();

        GenerateContentConfig config =
                GenerateContentConfig.builder()
                        .responseMimeType("application/json")
                        .responseSchema(rootSchema)
                        .build();

        GenerateContentResponse response =
                geminiClient.models.generateContent(
                        "gemini-2.5-flash",
                        prompt,
                        config
                );

        String json = response.text();

        if (json == null || json.isBlank()) {
            throw new IllegalStateException(
                    "AI returned an empty response."
            );
        }

        json = json.trim();

        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }

        if (json.endsWith("```")) {
            json =
                    json.substring(
                            0,
                            json.length() - 3
                    );
        }

        JsonNode root =
                objectMapper.readTree(json.trim());

        JsonNode data =
                root.has("data")
                        ? root.path("data")
                        : root;

        JsonNode aiCriteriaV2 =
                data.path(
                        "investmentCriteriaDTOV2s"
                );

        for (int i = 0; i < criteriaList.size(); i++) {

            var hardCriteria =
                    criteriaList.get(i);

            var reqCriteria =
                    request.getCriteriaList().get(i);

            JsonNode aiCrit =
                    aiCriteriaV2.path(i);

            JsonNode aiPropertiesNode =
                    aiCrit.path(
                            "proposedPropertyDTOList"
                    );

            List<ProposedPropertyDTO> resultProperties =
                    new ArrayList<>();

            for (
                    int j = 0;
                    j < aiPropertiesNode.size();
                    j++
            ) {

                JsonNode propNode =
                        aiPropertiesNode.get(j);

                long aiListingId =
                        propNode
                                .path("listingId")
                                .asLong();

                ProposedPropertyDTO matchedDto =
                        hardCriteria
                                .getProposedPropertyDTOList()
                                .stream()
                                .filter(
                                        p ->
                                                p.getListingId() != null
                                                        && p.getListingId()
                                                        .longValue()
                                                        == aiListingId
                                )
                                .findFirst()
                                .orElse(null);

                if (matchedDto != null) {

                    JsonNode metrics =
                            propNode.path(
                                    "financialMetrics"
                            );

                    double valuePrice =
                            matchedDto.getValuePrice() != null
                                    ? matchedDto.getValuePrice()
                                    : 0.0;

                    double loanCapitalD =
                            request.getLoanCapital() != null
                                    ? request.getLoanCapital()
                                    : 0.0;

                    int loanTermMonths =
                            loanTermYears * 12;

                    double monthlyPI = 0.0;

                    if (loanCapitalD > 0) {

                        double r =
                                0.07 / 12.0;

                        monthlyPI =
                                loanCapitalD
                                        * (
                                        r
                                                * Math.pow(
                                                1 + r,
                                                loanTermMonths
                                        )
                                )
                                        / (
                                        Math.pow(
                                                1 + r,
                                                loanTermMonths
                                        ) - 1
                                );
                    }

                    double rentalCashflow = 0.0;
                    double netCashflow = 0.0;
                    double estimatedProfit = 0.0;

                    if (isDongTien) {

                        rentalCashflow =
                                metrics
                                        .path(
                                                "monthlyRentalCashflow"
                                        )
                                        .asDouble(0);

                        if (
                                rentalCashflow <= 0
                                        && valuePrice > 0
                        ) {

                            int propTypeId =
                                    reqCriteria.getPropertyTypeId() != null
                                            ? reqCriteria.getPropertyTypeId()
                                            : 0;

                            double yield =
                                    switch (propTypeId) {
                                        case 1 -> 0.028;
                                        case 2 -> 0.0;
                                        case 3 -> 0.050;
                                        case 4 -> 0.075;
                                        case 5 -> 0.040;
                                        case 6 -> 0.060;
                                        default -> 0.040;
                                    };

                            rentalCashflow =
                                    (valuePrice * yield)
                                            / 12.0;
                        }

                        netCashflow =
                                rentalCashflow
                                        - monthlyPI;

                        double yield =
                                valuePrice > 0
                                        ? (
                                        rentalCashflow
                                                * 12.0
                                )
                                        / valuePrice
                                        : 0.04;

                        estimatedProfit =
                                valuePrice
                                        * Math.pow(
                                        1 + 0.07 + yield,
                                        5
                                )
                                        - valuePrice;

                    } else if (isMuaSuaBan) {

                        rentalCashflow = 0.0;
                        netCashflow = 0.0;

                        double additionalCost =
                                additionalCostReq > 0
                                        ? additionalCostReq
                                        : valuePrice * 0.05;

                        estimatedProfit =
                                (
                                        valuePrice
                                                * Math.pow(
                                                1 + 0.20,
                                                0.75
                                        )
                                                - valuePrice
                                )
                                        - additionalCost;

                    } else {

                        rentalCashflow = 0.0;
                        netCashflow = 0.0;

                        estimatedProfit =
                                valuePrice
                                        * Math.pow(
                                        1 + 0.08,
                                        0.25
                                )
                                        - valuePrice;
                    }

                    double roiPercentage =
                            valuePrice > 0
                                    ? (
                                    estimatedProfit
                                            / valuePrice
                            )
                                    * 100.0
                                    : 0.0;

                    long roundedRentalCashflow =
                            Math.round(
                                    rentalCashflow
                            );

                    long roundedNetCashflow =
                            Math.round(
                                    netCashflow
                            );

                    matchedDto.setFinancialMetrics(
                            FinancialMetricsDTO.builder()
                                    .estimatedProfit(
                                            Math.round(
                                                    estimatedProfit
                                            )
                                    )
                                    .monthlyRentalCashflow(
                                            roundedRentalCashflow != 0
                                                    ? roundedRentalCashflow
                                                    : null
                                    )
                                    .monthlyPrincipalInterest(
                                            Math.round(
                                                    monthlyPI
                                            )
                                    )
                                    .netCashflow(
                                            roundedNetCashflow != 0
                                                    ? roundedNetCashflow
                                                    : null
                                    )
                                    .roiPercentage(
                                            Math.round(
                                                    roiPercentage
                                                            * 100.0
                                            )
                                                    / 100.0
                                    )
                                    .build()
                    );

                    double propertyEquity =
                            Math.min(
                                    request.getEquity() != null
                                            ? request.getEquity()
                                            : 0L,
                                    valuePrice
                            );

                    double propertyLoan =
                            Math.max(
                                    valuePrice - propertyEquity,
                                    0.0
                            );

                    matchedDto.setScenarios(
                            buildPropertyScenarios(
                                    valuePrice,
                                    propertyLoan,
                                    propertyEquity,
                                    loanTermYears,
                                    rentalCashflow,
                                    request.getCurrentCashFlow(),
                                    additionalCostReq,
                                    isDongTien,
                                    isMuaSuaBan,
                                    isLuotSong,
                                    holdingMonths
                            )
                    );

                    resultProperties.add(
                            matchedDto
                    );
                }
            }

            hardCriteria.setProposedPropertyDTOList(
                    resultProperties
            );
        }

        return InvestmentPlanDTO.builder()
                .totalCapital(totalCapital)
                .loanCapital(request.getLoanCapital())
                .equity(request.getEquity())
                .strategyName(strategyName)
                .investmentCriteriaDTOV2s(criteriaList)
                .build();
    }

    private List<PropertyScenarioDTO> buildPropertyScenarios(
            double valuePrice,
            double propertyLoan,
            double propertyEquity,
            int loanTermYears,
            double rentalIncome,
            Long currentCashFlow,
            long additionalCost,
            boolean isDongTien,
            boolean isMuaSuaBan,
            boolean isLuotSong,
            int holdingMonths
    ) {

        List<PropertyScenarioDTO> scenarios =
                new ArrayList<>();

        PropertyScenarioDTO greenScenario =
                calculatePropertyScenario(
                        "XANH",
                        valuePrice,
                        propertyLoan,
                        propertyEquity,
                        loanTermYears,
                        rentalIncome,
                        currentCashFlow,
                        additionalCost,
                        isDongTien,
                        isMuaSuaBan,
                        isLuotSong,
                        holdingMonths,
                        OPTIMISTIC_INTEREST_RATE,
                        1.0,
                        GAIN_RATE_OPTIMISTIC,
                        1.0
                );

        PropertyScenarioDTO redScenario =
                calculatePropertyScenario(
                        "DO",
                        valuePrice,
                        propertyLoan,
                        propertyEquity,
                        loanTermYears,
                        rentalIncome,
                        currentCashFlow,
                        additionalCost,
                        isDongTien,
                        isMuaSuaBan,
                        isLuotSong,
                        holdingMonths * 2,
                        PESSIMISTIC_INTEREST_RATE,
                        0.5,
                        0.0,
                        1.30
                );

        scenarios.add(greenScenario);
        scenarios.add(redScenario);

        return scenarios;
    }

    private PropertyScenarioDTO calculatePropertyScenario(
            String scenarioType,
            double valuePrice,
            double propertyLoan,
            double propertyEquity,
            int loanTermYears,
            double rentalIncome,
            Long currentCashFlow,
            long additionalCost,
            boolean isDongTien,
            boolean isMuaSuaBan,
            boolean isLuotSong,
            int holdingMonths,
            double annualInterestRate,
            double occupancyRate,
            double priceGrowthRate,
            double capexMultiplier
    ) {

        int loanTermMonths =
                loanTermYears * 12;

        double monthlyPayment =
                calculateMonthlyPayment(
                        propertyLoan,
                        annualInterestRate,
                        loanTermMonths
                );

        double monthlyCashflowIn = 0.0;
        double netCashflow = 0.0;
        double survivalCashflow = 0.0;
        double capex = 0.0;

        double userIncome =
                currentCashFlow != null
                        ? currentCashFlow
                        : 0.0;

        if (isLuotSong) {

            monthlyCashflowIn = 0.0;

            capex = 0.0;

            netCashflow =
                    -monthlyPayment;

            survivalCashflow =
                    userIncome
                            - monthlyPayment;
        }

        if (isMuaSuaBan) {

            monthlyCashflowIn = 0.0;

            double baseCapex =
                    additionalCost > 0
                            ? additionalCost
                            : valuePrice * CAPEX_RATE;

            capex =
                    baseCapex
                            * capexMultiplier;

            netCashflow =
                    -monthlyPayment;

            survivalCashflow =
                    userIncome
                            - monthlyPayment;
        }

        if (isDongTien) {

            monthlyCashflowIn =
                    rentalIncome
                            * occupancyRate;

            netCashflow =
                    monthlyCashflowIn
                            - monthlyPayment;

            survivalCashflow =
                    userIncome
                            + netCashflow;

            capex = 0.0;
        }

        double sellingPrice =
                valuePrice
                        * (
                        1 + priceGrowthRate
                );

        double gain =
                sellingPrice
                        - valuePrice
                        - (
                        sellingPrice
                                * TRANSACTION_COST_RATE
                );

        double totalNetProfit = 0.0;

        if (isLuotSong) {

            totalNetProfit =
                    gain
                            - (
                            monthlyPayment
                                    * holdingMonths
                    );
        }

        if (isMuaSuaBan) {

            totalNetProfit =
                    gain
                            - capex
                            - (
                            monthlyPayment
                                    * holdingMonths
                    );
        }

        if (isDongTien) {

            totalNetProfit =
                    gain
                            + (
                            netCashflow
                                    * holdingMonths
                    )
                            - capex;
        }

        double roePercentage =
                propertyEquity > 0
                        ? (
                        totalNetProfit
                                / propertyEquity
                )
                        * 100.0
                        : 0.0;

        String riskLabel =
                survivalCashflow >= 0
                        ? "SAFE"
                        : "DANGER";

        double savingBenchmark =
                SAVING_INTEREST_RATE_PER_YEAR
                        * (
                        (double) holdingMonths / 12.0
                )
                        * 100.0;

        boolean isWorthInvesting =
                roePercentage
                        > savingBenchmark;

        return PropertyScenarioDTO.builder()
                .scenarioType(scenarioType)
                .interestRate(
                        annualInterestRate
                                * 100.0
                )
                .occupancyRate(
                        occupancyRate
                                * 100.0
                )
                .monthlyPayment(
                        Math.round(
                                monthlyPayment
                        )
                )
                .monthlyCashflowIn(
                        Math.round(
                                monthlyCashflowIn
                        )
                )
                .netCashflow(
                        Math.round(
                                netCashflow
                        )
                )
                .survivalCashflow(
                        Math.round(
                                survivalCashflow
                        )
                )
                .totalNetProfit(
                        Math.round(
                                totalNetProfit
                        )
                )
                .roiPercentage(
                        Math.round(
                                roePercentage
                                        * 100.0
                        )
                                / 100.0
                )
                .riskLabel(riskLabel)
                .isWorthInvesting(
                        isWorthInvesting
                )
                .build();
    }

    private double calculateMonthlyPayment(
            double principal,
            double annualInterestRate,
            int loanTermMonths
    ) {

        if (
                principal <= 0
                        || loanTermMonths <= 0
        ) {
            return 0.0;
        }

        double monthlyInterestRate =
                annualInterestRate
                        / 12.0;

        double factor =
                Math.pow(
                        1 + monthlyInterestRate,
                        loanTermMonths
                );

        return principal
                * (
                monthlyInterestRate
                        * factor
        )
                / (
                factor - 1
        );
    }

    private int getDefaultHoldingMonths(String strategyName) {
        if (strategyName == null) {
            throw new IllegalArgumentException("Strategy name cannot be null");
        }

        String normalizedName = strategyName.trim().toLowerCase();

        if (normalizedName.contains("đầu cơ lướt sóng")
                || normalizedName.contains("dau co luot song")) {
            return HOLDING_MONTHS_LUOT_SONG;
        }

        if (normalizedName.contains("mua sửa bán")
                || normalizedName.contains("mua sua ban")) {
            return HOLDING_MONTHS_MUA_SUA_BAN;
        }

        if (normalizedName.contains("bđs dòng tiền")
                || normalizedName.contains("bds dong tien")
                || normalizedName.contains("dòng tiền")) {
            return HOLDING_MONTHS_DONG_TIEN;
        }

        throw new IllegalArgumentException(
                "Unsupported investment strategy: " + strategyName
        );
    }

    private InvestmentProfileVersion saveNewInvestmentPlan(Investor investor, InvestmentPlanRequest request, InvestmentPlanDTO output, Strategy strategy) throws Exception {
        LocalDateTime now = LocalDateTime.now();

        String wardInfo = request.getWardNames() != null && !request.getWardNames().isEmpty()
                ? " " + request.getWardNames().get(0) : "";
        String baseProfileName = "Kế hoạch " + strategy.getName() + wardInfo;

        InvestmentProfile profile = InvestmentProfile.builder()
                .investor(investor)
                .name(baseProfileName)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .profileVersions(new ArrayList<>())
                .build();

        InvestmentProfile savedProfile = investmentProfileRepository.save(profile);

        return saveProfileVersion(savedProfile, baseProfileName + " - Phiên bản 1", request, output, strategy, now);
    }

    private InvestmentProfileVersion saveUpdateInvestmentPlan(InvestmentProfile existingProfile, InvestmentPlanRequest request, InvestmentPlanDTO output, Strategy strategy) throws Exception {
        LocalDateTime now = LocalDateTime.now();

        long currentVersionsCount = investmentProfileVersionRepository
                .countByInvestmentProfile_InvestmentProfileId(existingProfile.getInvestmentProfileId());

        String baseProfileName = existingProfile.getName() != null ? existingProfile.getName() : "Kế hoạch " + strategy.getName();
        String autoVersionName = baseProfileName + " - Phiên bản " + (currentVersionsCount + 1);

        return saveProfileVersion(existingProfile, autoVersionName, request, output, strategy, now);
    }

    private InvestmentProfileVersion saveProfileVersion(InvestmentProfile profile, String versionName, InvestmentPlanRequest request, InvestmentPlanDTO output, Strategy strategy, LocalDateTime now) throws Exception {
        long totalCapitalCalculated = request.getEquity() + request.getLoanCapital();

        InvestmentProfileVersion versionEntity = InvestmentProfileVersion.builder()
                .investmentProfile(profile)
                .profileVersionName(versionName)
                .strategy(strategy)
                .equity(request.getEquity())
                .loanCapital(request.getLoanCapital())
                .currentCashflow(request.getCurrentCashFlow())
                .conscious(request.getConsciousName())
                .longTermYear(request.getLongTermYear())
                .wards(request.getWardNames() != null ? request.getWardNames() : new ArrayList<>())
                .totalCapital(totalCapitalCalculated)
                .investmentStrategyDetail(request.getInvestmentStrategyDetail())
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .investmentCriterias(new ArrayList<>())
                .build();

        InvestmentProfileVersion savedVersion = investmentProfileVersionRepository.save(versionEntity);

        if (request.getCriteriaList() != null && !request.getCriteriaList().isEmpty()) {
            List<InvestmentCriteriaDTOV2> aiCriteriaList = (output != null && output.getInvestmentCriteriaDTOV2s() != null)
                    ? output.getInvestmentCriteriaDTOV2s() : new ArrayList<>();

            for (int i = 0; i < request.getCriteriaList().size(); i++) {
                var critReq = request.getCriteriaList().get(i);
                PropertyType pType = propertyTypeRepository.findById(critReq.getPropertyTypeId()).orElse(null);

                InvestmentCriteria criteriaEntity = new InvestmentCriteria();
                criteriaEntity.setInvestmentProfileVersion(savedVersion);
                criteriaEntity.setPropertyType(pType);
                InvestmentCriteria savedCriteria = investmentCriteriaRepository.save(criteriaEntity);

                if (i < aiCriteriaList.size()) {
                    var critDTO = aiCriteriaList.get(i);
                    critDTO.setInvestmentCriteriaId(savedCriteria.getInvestmentCriteriaId());
                    if (pType != null) critDTO.setPropertyTypeName(pType.getName());

                    if (critDTO.getProposedPropertyDTOList() != null) {
                        for (var propDTO : critDTO.getProposedPropertyDTOList()) {
                            FinancialMetricsDTO mDTO = propDTO.getFinancialMetrics();

                            ProposedProperty propEntity = new ProposedProperty();
                            propEntity.setInvestmentCriteria(savedCriteria);
                            propEntity.setListingId(propDTO.getListingId() != null ? propDTO.getListingId() : 0);
                            propEntity.setProposalType(propDTO.getProposalType());
                            propEntity.setPropertyProjectName(propDTO.getPropertyProjectName());
                            propEntity.setArea(propDTO.getArea());
                            propEntity.setValuePrice(propDTO.getValuePrice());
                            propEntity.setDescription(propDTO.getDescription());
                            propEntity.setCreatedAt(now);

                            if (mDTO != null) {
                                propEntity.setEstimatedProfit(mDTO.getEstimatedProfit());
                                propEntity.setMonthlyRentalCashflow(mDTO.getMonthlyRentalCashflow());
                                propEntity.setMonthlyPrincipalInterest(mDTO.getMonthlyPrincipalInterest());
                                propEntity.setNetCashflow(mDTO.getNetCashflow());
                                propEntity.setRoiPercentage(mDTO.getRoiPercentage());
                            }

                            ProposedProperty savedProperty = proposedPropertyRepository.save(propEntity);
                            propDTO.setProposedPropertyId(savedProperty.getProposedPropertyId());

                            if (propDTO.getScenarios() != null) {
                                for (var scenarioDTO : propDTO.getScenarios()) {
                                    PropertyScenario scenarioEntity = new PropertyScenario();
                                    scenarioEntity.setProposedProperty(savedProperty);
                                    scenarioEntity.setScenarioType(scenarioDTO.getScenarioType());
                                    scenarioEntity.setInterestRate(scenarioDTO.getInterestRate());
                                    scenarioEntity.setOccupancyRate(scenarioDTO.getOccupancyRate());
                                    scenarioEntity.setMonthlyPayment(scenarioDTO.getMonthlyPayment());
                                    scenarioEntity.setMonthlyCashflowIn(scenarioDTO.getMonthlyCashflowIn());
                                    scenarioEntity.setNetCashflow(scenarioDTO.getNetCashflow());
                                    scenarioEntity.setSurvivalCashflow(scenarioDTO.getSurvivalCashflow());
                                    scenarioEntity.setTotalNetProfit(scenarioDTO.getTotalNetProfit());
                                    scenarioEntity.setRoiPercentage(scenarioDTO.getRoiPercentage());
                                    scenarioEntity.setRiskLabel(scenarioDTO.getRiskLabel());
                                    scenarioEntity.setIsWorthInvesting(scenarioDTO.getIsWorthInvesting());
                                    scenarioEntity.setCreatedAt(now);
                                    propertyScenarioRepository.save(scenarioEntity);
                                }
                            }
                        }
                    }
                }
            }
        }

        return savedVersion;
    }
    private void addProperty(Listing listing, String proposalType, List<ProposedPropertyDTO> properties, List<Map<String, Object>> aiProperties) {
        Property property = listing.getProperty();

        String condition = property != null && property.getPropertyCondition() != null
                ? property.getPropertyCondition().getName() : "Unknown";

        String ward = property != null && property.getLocation() != null && property.getLocation().getWard() != null
                ? property.getLocation().getWard().getName() : "Unknown";

        properties.add(ProposedPropertyDTO.builder()
                .listingId(listing.getListingId())
                .proposalType(proposalType)
                .propertyProjectName(listing.getTitle())
                .area(property != null && property.getArea() != null ? property.getArea().intValue() : 0)
                .valuePrice(listing.getPrice() != null ? listing.getPrice().doubleValue() : 0.0)
                .description(listing.getDescription())
                .build());

        aiProperties.add(Map.of(
                "listingId", listing.getListingId(),
                "title", listing.getTitle(),
                "valuePrice", listing.getPrice() != null ? listing.getPrice() : 0,
                "area", property != null && property.getArea() != null ? property.getArea() : 0.0,
                "propertyCondition", condition,
                "wardName", ward,
                "proposalType", proposalType
        ));
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

            InvestmentProfile profile = version.getInvestmentProfile();

            if (profile != null) {
                boolean hasActiveVersion = investmentProfileVersionRepository
                        .existsByInvestmentProfile_InvestmentProfileIdAndIsActiveTrue(
                                profile.getInvestmentProfileId()
                        );
                if (!hasActiveVersion) {
                    profile.setIsActive(false);
                    profile.setUpdatedAt(LocalDateTime.now());
                    investmentProfileRepository.save(profile);
                }
            }

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
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.success(null, "Id does not exist"));
            }
            for (InvestmentProfileVersion version : investmentProfile.getProfileVersions()) {
                version.setIsActive(false);
                investmentProfileVersionRepository.save(version);
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