package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyValuation;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.AccountVerificationDTOV2;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PropertyValuationDTO;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import java.util.*;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.CertificationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.PropertyValuationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.VerificationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.ListingStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.AccountVerification;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Listing;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountVerificationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PropertyValuationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.StaffDashboardKpiDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PendingListingDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.StaffDashboardServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StaffDashboardServiceImplement implements StaffDashboardServiceInterface {
    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private AccountVerificationRepository accountVerificationRepository;

    @Autowired
    private PropertyValuationRepository propertyValuationRepository;

    public ResponseEntity<ApiResponse> getDashBoardKpi() {
        try{
            long listing_count_pending = 0;
            long listing_count_certification = 0;
            long account_verification_count = 0;
            long property_valuation_count = 0;

            listing_count_pending = listingRepository.findAll().stream()
                    .filter(listing -> Boolean.TRUE.equals(listing.getIsVerified()
                            &&Boolean.TRUE.equals(listing.getIsActive())))
                    .count();
            listing_count_certification = listingRepository.findAll().stream()
                    .filter(listing -> Boolean.TRUE.equals(listing.getIsActive())
                            && listing.getCertificationStatus() == CertificationStatusEnum.PENDING)
                    .count();

            account_verification_count = accountVerificationRepository.findAll().stream()
                    .filter(accountVerification -> Boolean.TRUE.equals(accountVerification.getIsActive())
                            && accountVerification.getVerificationStatus() == VerificationStatusEnum.PENDING)
                    .count();

            property_valuation_count = propertyValuationRepository.findAll().stream()
                    .filter(propertyValuation -> Boolean.TRUE.equals(propertyValuation.getIsActive()
                            && propertyValuation.getPropertyValuationStatus() == PropertyValuationStatusEnum.PENDING))
                    .count();

            StaffDashboardKpiDTO staffDashboardKpiDTO = new StaffDashboardKpiDTO();
            staffDashboardKpiDTO.setPendingListingsCount(listing_count_pending);
            staffDashboardKpiDTO.setPendingAccountVerificationsCount(account_verification_count);
            staffDashboardKpiDTO.setPendingPropertyValuationsCount(property_valuation_count);
            staffDashboardKpiDTO.setPendingListingCertificationsCount(listing_count_certification);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(staffDashboardKpiDTO, "Staff dashboard kpi"));

        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.toString(), "Server errors"));
        }
    }

    private PendingListingDTO convertToSummaryDTO(Listing listing) {
        PendingListingDTO dto = new PendingListingDTO();
        dto.setListingId(Long.valueOf(listing.getListingId()));
        dto.setTitle(listing.getTitle());

        String sellerName = "Ẩn danh";
        if (listing.getContactPersonName() != null) {
            sellerName = listing.getContactPersonName();
        } else if (listing.getSeller() != null && listing.getSeller().getAccount() != null) {
            sellerName = listing.getSeller().getAccount().getFull_name();
        }
        dto.setSellerName(sellerName);

        dto.setCreatedAt(listing.getCreatedAt());
        dto.setVerificationStatus(listing.getListingVerification() != null && listing.getListingVerification().getStatus() != null
                ? listing.getListingVerification().getStatus().name()
                : "PENDING");
        return dto;
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getPendingListing(int page, int size) {
        try {
            List<Listing> allListings = listingRepository.findAll();
            if (allListings == null) {
                allListings = Collections.emptyList();
            }

            List<Listing> sortedList = allListings.stream()
                    .filter(listing -> Boolean.TRUE.equals(listing.getIsActive())
                            && listing.getListingVerification() != null
                            && listing.getListingVerification().getStatus() == ListingStatusEnum.PENDING)
                    .sorted(Comparator.comparing(
                            Listing::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .toList();

            boolean isGetAll = (page == 0 && size == 0);

            List<PendingListingDTO> pagedContent;
            int effectivePage = 0;
            int effectiveSize = sortedList.size();
            int totalElements = sortedList.size();
            int totalPages = 1;
            boolean isLast = true;

            if (isGetAll) {
                pagedContent = sortedList.stream().map(this::convertToSummaryDTO).collect(Collectors.toList());
            } else {
                effectiveSize = size > 0 ? size : 20;
                effectivePage = Math.max(page, 0);

                int offset = effectivePage * effectiveSize;
                totalPages = (int) Math.ceil((double) totalElements / effectiveSize);
                if (totalPages == 0) totalPages = 1;
                isLast = effectivePage >= totalPages - 1;

                List<Listing> slicedList = sortedList.stream()
                        .skip(offset)
                        .limit(effectiveSize)
                        .toList();

                pagedContent = slicedList.stream().map(this::convertToSummaryDTO).collect(Collectors.toList());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", pagedContent);
            result.put("page", effectivePage);
            result.put("size", effectiveSize);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("last", isLast);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(result, "Get pending listings successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    private AccountVerificationDTOV2 convertToAccountVerificationDTOV2(AccountVerification av) {
        AccountVerificationDTOV2 dto = new AccountVerificationDTOV2();
        dto.setVerificationId(av.getAccountVerificationId());
        if (av.getAccount() != null) {
            dto.setAccountId(av.getAccount().getAccountId());
            dto.setFullName(av.getAccount().getFull_name());
            dto.setRole(av.getAccount().getRole() != null ? av.getAccount().getRole().name() : "Seller");
        } else {
            dto.setFullName("Ẩn danh");
        }
        dto.setSubmittedAt(av.getCreatedAt());
        dto.setStatus(av.getVerificationStatus() != null ? av.getVerificationStatus().name() : "PENDING");
        return dto;
    }

    private PropertyValuationDTO convertToPropertyValuationDTO(PropertyValuation pv) {
        PropertyValuationDTO dto = new PropertyValuationDTO();
        dto.setValuationRequestId(pv.getPropertyValuationId());
        if (pv.getProperty() != null) {
            dto.setPropertyId(pv.getProperty().getPropertyId());
            dto.setAddress(pv.getProperty().getAddressParticular());
        }
        dto.setRequestedAt(pv.getCreatedAt());
        dto.setStatus(pv.getPropertyValuationStatus() != null ? pv.getPropertyValuationStatus().name() : "PENDING");
        return dto;
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getPendingAccountVerifications(int page, int size) {
        try {
            List<AccountVerification> allVerifications = accountVerificationRepository.findAll();
            if (allVerifications == null) {
                allVerifications = Collections.emptyList();
            }

            List<AccountVerification> sortedList = allVerifications.stream()
                    .filter(av -> Boolean.TRUE.equals(av.getIsActive())
                            && av.getVerificationStatus() == VerificationStatusEnum.PENDING)
                    .sorted(Comparator.comparing(
                            AccountVerification::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .toList();

            boolean isGetAll = (page == 0 && size == 0);

            List<AccountVerificationDTOV2> pagedContent;
            int effectivePage = 0;
            int effectiveSize = sortedList.size();
            int totalElements = sortedList.size();
            int totalPages = 1;
            boolean isLast = true;

            if (isGetAll) {
                pagedContent = sortedList.stream().map(this::convertToAccountVerificationDTOV2).collect(Collectors.toList());
            } else {
                effectiveSize = size > 0 ? size : 20;
                effectivePage = Math.max(page, 0);

                int offset = effectivePage * effectiveSize;
                totalPages = (int) Math.ceil((double) totalElements / effectiveSize);
                if (totalPages == 0) totalPages = 1;
                isLast = effectivePage >= totalPages - 1;

                List<AccountVerification> slicedList = sortedList.stream()
                        .skip(offset)
                        .limit(effectiveSize)
                        .toList();

                pagedContent = slicedList.stream().map(this::convertToAccountVerificationDTOV2).collect(Collectors.toList());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", pagedContent);
            result.put("page", effectivePage);
            result.put("size", effectiveSize);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("last", isLast);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(result, "Get pending account verifications successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<ApiResponse> getPendingPropertyValuations(int page, int size) {
        try {
            List<PropertyValuation> allValuations = propertyValuationRepository.findAll();
            if (allValuations == null) {
                allValuations = Collections.emptyList();
            }

            List<PropertyValuation> sortedList = allValuations.stream()
                    .filter(pv -> Boolean.TRUE.equals(pv.getIsActive())
                            && pv.getPropertyValuationStatus() == PropertyValuationStatusEnum.PENDING)
                    .sorted(Comparator.comparing(
                            PropertyValuation::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ))
                    .toList();

            boolean isGetAll = (page == 0 && size == 0);

            List<PropertyValuationDTO> pagedContent;
            int effectivePage = 0;
            int effectiveSize = sortedList.size();
            int totalElements = sortedList.size();
            int totalPages = 1;
            boolean isLast = true;

            if (isGetAll) {
                pagedContent = sortedList.stream().map(this::convertToPropertyValuationDTO).collect(Collectors.toList());
            } else {
                effectiveSize = size > 0 ? size : 20;
                effectivePage = Math.max(page, 0);

                int offset = effectivePage * effectiveSize;
                totalPages = (int) Math.ceil((double) totalElements / effectiveSize);
                if (totalPages == 0) totalPages = 1;
                isLast = effectivePage >= totalPages - 1;

                List<PropertyValuation> slicedList = sortedList.stream()
                        .skip(offset)
                        .limit(effectiveSize)
                        .toList();

                pagedContent = slicedList.stream().map(this::convertToPropertyValuationDTO).collect(Collectors.toList());
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", pagedContent);
            result.put("page", effectivePage);
            result.put("size", effectiveSize);
            result.put("totalElements", totalElements);
            result.put("totalPages", totalPages);
            result.put("last", isLast);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(result, "Get pending property valuations successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}