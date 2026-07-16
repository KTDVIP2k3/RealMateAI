package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.VerificationStatusEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.AccountVerification;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Seller;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountVerificationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.SellerRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AccountVerificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AccountVerificationUpdateRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.AccountVerificationDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.AccountVerificationListDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.AccountVerificationServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountVerificationServiceImplement implements AccountVerificationServiceInterface {

    @Autowired
    private AccountVerificationRepository accountVerificationRepository;

    @Autowired
    private CloudinaryMediaServiceImplement cloudinaryMediaService;

    @Autowired
    private AuthenUntil authenUntil;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private SellerRepository sellerRepository;

    @Override
    public ResponseEntity<ApiResponse> getAccountVerificationByStaffOrAdmin() {
        try {
            List<AccountVerification> list = accountVerificationRepository.findAll();
            if (list.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(list, "Account verification list is empty"));
            }
            List<AccountVerificationListDTO> dtoList = list.stream()
                    .map(verification -> modelMapper.map(verification, AccountVerificationListDTO.class))
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(dtoList, "Get verification list successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createAccountVerification(AccountVerificationRequest request) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", "User authentication required"));
            }

            String cccdmtUrl = "";
            String cccdmsUrl = "";
            String selfieUrl = "";
            String businessLicenseUrl = "";

            if (request.getCccdmt() != null && !request.getCccdmt().isEmpty()) {
                cccdmtUrl = cloudinaryMediaService.uploadImage(request.getCccdmt());
            }
            if (request.getCccdms() != null && !request.getCccdms().isEmpty()) {
                cccdmsUrl = cloudinaryMediaService.uploadImage(request.getCccdms());
            }
            if (request.getSelfie() != null && !request.getSelfie().isEmpty()) {
                selfieUrl = cloudinaryMediaService.uploadImage(request.getSelfie());
            }
            if (request.getBusinessLicense() != null && !request.getBusinessLicense().isEmpty()) {
                businessLicenseUrl = cloudinaryMediaService.uploadImage(request.getBusinessLicense());
            }

            AccountVerification.AccountVerificationBuilder verificationBuilder = AccountVerification.builder()
                    .account(currentAccount)
                    .cccdmt(cccdmtUrl)
                    .cccdms(cccdmsUrl)
                    .selfie(selfieUrl)
                    .businessLicense(businessLicenseUrl)
                    .verificationStatus(VerificationStatusEnum.PENDING)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now());

            if (currentAccount.getSeller() != null) {
                verificationBuilder.seller(currentAccount.getSeller());
            }else {
                Seller seller = new Seller();
                seller.setIsActive(true);
                seller.setCreatedAt(LocalDateTime.now());
                seller.setAccount(currentAccount);
                seller = sellerRepository.save(seller);

                currentAccount.setSeller(seller);
                verificationBuilder.seller(seller);
            }

            AccountVerification verification = verificationBuilder.build();
            accountVerificationRepository.save(verification);

            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null, "Create verification successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getAccountVerificationByIdByStaffOrAdmin(Integer id) {
        try {
            AccountVerification verification = accountVerificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Account verification not found with id: " + id));
            AccountVerificationDTO dto = modelMapper.map(verification, AccountVerificationDTO.class);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(dto, "Get verification detail successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getAccountVerificationForUser() {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", "User authentication required"));
            }
            List<AccountVerification> list = accountVerificationRepository.findAll().stream()
                    .filter(v -> v.getAccount() != null && currentAccount.getAccountId() == v.getAccount().getAccountId())
                    .collect(Collectors.toList());
            if (list.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(list, "Your account verification list is empty"));
            }
            List<AccountVerificationListDTO> dtoList = list.stream()
                    .map(verification -> modelMapper.map(verification, AccountVerificationListDTO.class))
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(dtoList, "Get your verification list successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> getAccountVerificationDetailForUser(Integer id) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", "User authentication required"));
            }
            AccountVerification verification = accountVerificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Account verification not found with id: " + id));
            if (verification.getAccount() == null || currentAccount.getAccountId() != verification.getAccount().getAccountId()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.fail("Forbidden", "You do not have permission to view this verification"));
            }
            AccountVerificationDTO dto = modelMapper.map(verification, AccountVerificationDTO.class);
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(dto, "Get your verification detail successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> updateAccountVerification(AccountVerificationUpdateRequest request) {
        try {
            Account currentAccount = authenUntil.getCurrentUSer();
            if (currentAccount == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail("Unauthorized", "User authentication required"));
            }

            AccountVerification verification = accountVerificationRepository.findById(request.getVerificationId().intValue())
                    .orElseThrow(() -> new RuntimeException("Account verification not found with id: " + request.getVerificationId()));

            if (request.getCccdmt() != null && !request.getCccdmt().isEmpty()) {
                String newCccdmtUrl = cloudinaryMediaService.updateImage(request.getCccdmt(), verification.getCccdmt());
                verification.setCccdmt(newCccdmtUrl);
            }

            if (request.getCccdms() != null && !request.getCccdms().isEmpty()) {
                String newCccdmsUrl = cloudinaryMediaService.updateImage(request.getCccdms(), verification.getCccdms());
                verification.setCccdms(newCccdmsUrl);
            }

            if (request.getBusinessLicense() != null && !request.getBusinessLicense().isEmpty()) {
                String newBusinessLicenseUrl = cloudinaryMediaService.updateImage(request.getBusinessLicense(), verification.getBusinessLicense());
                verification.setBusinessLicense(newBusinessLicenseUrl);
            }

            if (request.getSelfie() != null && !request.getSelfie().isEmpty()) {
                String newSelfieUrl = cloudinaryMediaService.updateImage(request.getSelfie(), verification.getSelfie());
                verification.setSelfie(newSelfieUrl);
            }

            verification.setAccount(currentAccount);

            if (currentAccount.getSeller() != null) {
                verification.setSeller(currentAccount.getSeller());
            }

            verification.setUpdatedAt(LocalDateTime.now());
            accountVerificationRepository.save(verification);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Update verification successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> approveAccountVerification(Integer id) {
        try {
            AccountVerification verification = accountVerificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Account verification not found with id: " + id));

            verification.setVerificationStatus(VerificationStatusEnum.APPROVED);
            verification.setReason(null);
            verification.setUpdatedAt(LocalDateTime.now());
            accountVerificationRepository.save(verification);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Approved verification successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> rejectAccountVerification(Integer id, String reason) {
        try {
            AccountVerification verification = accountVerificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Account verification not found with id: " + id));

            verification.setVerificationStatus(VerificationStatusEnum.REJECTED);
            verification.setReason(reason);
            verification.setUpdatedAt(LocalDateTime.now());
            accountVerificationRepository.save(verification);

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Rejected verification successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}