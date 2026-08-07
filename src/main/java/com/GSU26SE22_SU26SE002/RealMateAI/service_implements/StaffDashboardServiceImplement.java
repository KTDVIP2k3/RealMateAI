package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.CertificationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.PropertyValuationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.enums.VerificationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.AccountVerification;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountVerificationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ListingRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PropertyValuationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.StaffDashboardKpiDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.StaffDashboardServiceInterface;
import com.cloudinary.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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

    public ResponseEntity<ApiResponse> getPendingListing(int page, int size){
        return null;
    }
}
