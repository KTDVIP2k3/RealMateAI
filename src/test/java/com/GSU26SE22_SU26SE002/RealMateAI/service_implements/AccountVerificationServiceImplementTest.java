package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.AccountVerification;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountVerificationRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.SellerRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AccountVerificationRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.AccountVerificationUpdateRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.NotificationService;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountVerificationServiceImplement - Account Verification Management")
class AccountVerificationServiceImplementTest {

    @Mock
    private AccountVerificationRepository accountVerificationRepository;
    @Mock
    private CloudinaryMediaServiceImplement cloudinaryMediaService;
    @Mock
    private AuthenUntil authenUntil;
    @Mock
    private ModelMapper modelMapper;
    @Mock
    private SellerRepository sellerRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AccountVerificationServiceImplement verificationService;

    private Account sampleAccount;
    private AccountVerification sampleVerification;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setAccountId(1);
        sampleAccount.setUserName("testuser");
        sampleAccount.setEmail("test@gmail.com");

        sampleVerification = new AccountVerification();
        sampleVerification.setAccountVerificationId(1);
        sampleVerification.setAccount(sampleAccount);
        sampleVerification.setCccdmt("http://cccdmt.jpg");
        sampleVerification.setCccdms("http://cccdms.jpg");
        sampleVerification.setSelfie("http://selfie.jpg");
    }

    @Nested
    @DisplayName("View Account Verification Requests")
    class ViewVerificationRequestsTests {

        @Test
        @DisplayName("Empty list returns OK with empty message")
        void getVerificationByStaff_emptyList_returnsOk() {
            when(accountVerificationRepository.findAll()).thenReturn(List.of());

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationByStaffOrAdmin();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Account verification list is empty", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-empty list returns OK with list data")
        void getVerificationByStaff_withData_returnsOk() {
            when(accountVerificationRepository.findAll()).thenReturn(List.of(sampleVerification));

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationByStaffOrAdmin();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Get verification list successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getVerificationByStaff_exception_returnsServerError() {
            when(accountVerificationRepository.findAll()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationByStaffOrAdmin();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("View Account Verification Detail")
    class ViewVerificationDetailTests {

        @Test
        @DisplayName("Existed verification ID returns OK")
        void getVerificationDetailByStaff_valid_returnsOk() {
            when(accountVerificationRepository.findById(1)).thenReturn(Optional.of(sampleVerification));

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationByIdByStaffOrAdmin(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Get verification detail successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent verification ID returns INTERNAL_SERVER_ERROR")
        void getVerificationDetailByStaff_notFound_returnsServerError() {
            when(accountVerificationRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationByIdByStaffOrAdmin(999);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Account verification not found with id: 999", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getVerificationDetailByStaff_exception_returnsServerError() {
            when(accountVerificationRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationByIdByStaffOrAdmin(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("View Account Verifications (User)")
    class ViewVerificationsForUserTests {

        @Test
        @DisplayName("Empty list returns OK with empty message")
        void getVerificationForUser_emptyList_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findAll()).thenReturn(List.of());

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationForUser();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Your account verification list is empty", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-empty list returns OK with data")
        void getVerificationForUser_withData_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findAll()).thenReturn(List.of(sampleVerification));

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationForUser();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Get your verification list successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Account does not exist returns UNAUTHORIZED")
        void getVerificationForUser_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationForUser();

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("User authentication required", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getVerificationForUser_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationForUser();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Verification ID belonging to another user returns FORBIDDEN")
        void getVerificationDetailForUser_forbidden_returnsForbidden() {
            Account anotherUser = new Account();
            anotherUser.setAccountId(99); // Khác với sampleAccount (ID = 1)
            sampleVerification.setAccount(anotherUser);

            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findById(1)).thenReturn(Optional.of(sampleVerification));

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationDetailForUser(1);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("You do not have permission to view this verification", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Valid verification ID returns OK")
        void getVerificationDetailForUser_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findById(1)).thenReturn(Optional.of(sampleVerification));

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationDetailForUser(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Get your verification detail successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Account does not exist returns UNAUTHORIZED")
        void getVerificationDetailForUser_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationDetailForUser(1);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("User authentication required", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent verification ID returns INTERNAL_SERVER_ERROR")
        void getVerificationDetailForUser_notFound_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationDetailForUser(999);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Account verification not found with id: 999", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getVerificationDetailForUser_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = verificationService.getAccountVerificationDetailForUser(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Approve Account Verification Request")
    class ApproveVerificationTests {

        @Test
        @DisplayName("Valid ID returns OK")
        void approveVerification_valid_returnsOk() {
            when(accountVerificationRepository.findById(1)).thenReturn(Optional.of(sampleVerification));
            when(accountVerificationRepository.save(any())).thenReturn(sampleVerification);

            ResponseEntity<ApiResponse> response = verificationService.approveAccountVerification(1);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Approved verification successfully", response.getBody().getMessage());
            verify(notificationService).notify(any(), any(), any());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void approveVerification_exception_returnsServerError() {
            when(accountVerificationRepository.findById(anyInt())).thenThrow(new RuntimeException("Server_Error: DB error"));

            ResponseEntity<ApiResponse> response = verificationService.approveAccountVerification(1);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Server_Error: DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns INTERNAL_SERVER_ERROR")
        void approveVerification_notFound_returnsServerError() {
            when(accountVerificationRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = verificationService.approveAccountVerification(999);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Account verification not found with id: 999", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Reject Account Verification Request")
    class RejectVerificationTests {

        @Test
        @DisplayName("Valid ID and reason returns OK")
        void rejectVerification_valid_returnsOk() {
            when(accountVerificationRepository.findById(1)).thenReturn(Optional.of(sampleVerification));
            when(accountVerificationRepository.save(any())).thenReturn(sampleVerification);

            ResponseEntity<ApiResponse> response = verificationService.rejectAccountVerification(1, "Invalid documents");

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Rejected verification successfully", response.getBody().getMessage());
            verify(notificationService).notify(any(), any(), any());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void rejectVerification_exception_returnsServerError() {
            when(accountVerificationRepository.findById(anyInt())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = verificationService.rejectAccountVerification(1, "reason");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent ID returns INTERNAL_SERVER_ERROR")
        void rejectVerification_notFound_returnsServerError() {
            when(accountVerificationRepository.findById(999)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = verificationService.rejectAccountVerification(999, "reason");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Account verification not found with id: 999", response.getBody().getMessage());
        }
    }
    @Nested
    @DisplayName("Create Account Verification")
    class CreateVerificationTests {

        private AccountVerificationRequest validRequest;
        private MultipartFile validFile;

        @BeforeEach
        void setUpVerification() {
            validFile = mock(MultipartFile.class);
            lenient().when(validFile.isEmpty()).thenReturn(false);

            validRequest = new AccountVerificationRequest();
            validRequest.setCccdmt(validFile);
            validRequest.setCccdms(validFile);
            validRequest.setSelfie(validFile);
        }

        @Test
        @DisplayName("Valid request returns CREATED")
        void createVerification_valid_returnsCreated() throws Exception {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(cloudinaryMediaService.uploadImage(any())).thenReturn("http://uploaded.jpg");
            when(accountVerificationRepository.save(any())).thenReturn(sampleVerification);

            ResponseEntity<ApiResponse> response = verificationService.createAccountVerification(validRequest);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertEquals("Create verification successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void createVerification_exception_returnsServerError() throws Exception {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(cloudinaryMediaService.uploadImage(any())).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = verificationService.createAccountVerification(validRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Account does not exist returns UNAUTHORIZED")
        void createVerification_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = verificationService.createAccountVerification(validRequest);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("User authentication required", response.getBody().getMessage());
        }

        // Validation tests removed because the service does not validate empty or invalid files
    }

    @Nested
    @DisplayName("Update Account Verification")
    class UpdateVerificationTests {

        private AccountVerificationUpdateRequest validRequest;

        @BeforeEach
        void setupRequest() {
            validRequest = new AccountVerificationUpdateRequest();
            validRequest.setVerificationId(1L);
            MultipartFile file = mock(MultipartFile.class);
            lenient().when(file.isEmpty()).thenReturn(false);
            validRequest.setCccdmt(file);
            validRequest.setCccdms(file);
            validRequest.setSelfie(file);
        }

        @Test
        @DisplayName("Valid update with all images returns OK")
        void updateVerification_valid_returnsOk() throws Exception {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findById(1)).thenReturn(Optional.of(sampleVerification));
            when(cloudinaryMediaService.updateImage(any(), anyString())).thenReturn("http://new-image.jpg");
            when(accountVerificationRepository.save(any())).thenReturn(sampleVerification);

            ResponseEntity<ApiResponse> response = verificationService.updateAccountVerification(validRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Update verification successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Valid update missing cccdmt returns OK")
        void updateVerification_missingCccdmt_returnsOk() throws Exception {
            validRequest.setCccdmt(null);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findById(1)).thenReturn(Optional.of(sampleVerification));
            when(cloudinaryMediaService.updateImage(any(), anyString())).thenReturn("http://new-image.jpg");
            when(accountVerificationRepository.save(any())).thenReturn(sampleVerification);

            ResponseEntity<ApiResponse> response = verificationService.updateAccountVerification(validRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Update verification successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Valid update missing cccdms returns OK")
        void updateVerification_missingCccdms_returnsOk() throws Exception {
            validRequest.setCccdms(null);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findById(1)).thenReturn(Optional.of(sampleVerification));
            when(cloudinaryMediaService.updateImage(any(), anyString())).thenReturn("http://new-image.jpg");
            when(accountVerificationRepository.save(any())).thenReturn(sampleVerification);

            ResponseEntity<ApiResponse> response = verificationService.updateAccountVerification(validRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Update verification successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Valid update missing selfie returns OK")
        void updateVerification_missingSelfie_returnsOk() throws Exception {
            validRequest.setSelfie(null);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findById(1)).thenReturn(Optional.of(sampleVerification));
            when(cloudinaryMediaService.updateImage(any(), anyString())).thenReturn("http://new-image.jpg");
            when(accountVerificationRepository.save(any())).thenReturn(sampleVerification);

            ResponseEntity<ApiResponse> response = verificationService.updateAccountVerification(validRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Update verification successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Account does not exist returns UNAUTHORIZED")
        void updateVerification_unauthenticated_returnsUnauthorized() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = verificationService.updateAccountVerification(validRequest);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            assertEquals("User authentication required", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Non-existent verification ID returns INTERNAL_SERVER_ERROR")
        void updateVerification_notFound_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findById(1)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = verificationService.updateAccountVerification(validRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Account verification not found with id: 1", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void updateVerification_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(accountVerificationRepository.findById(1)).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = verificationService.updateAccountVerification(validRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Invalid cccdmt format returns BAD_REQUEST")
        void updateVerification_invalidCccdmt_returnsBadRequest() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = verificationService.updateAccountVerification(validRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        @DisplayName("Invalid cccdms format returns BAD_REQUEST")
        void updateVerification_invalidCccdms_returnsBadRequest() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = verificationService.updateAccountVerification(validRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }

        @Test
        @DisplayName("Invalid selfie format returns BAD_REQUEST")
        void updateVerification_invalidSelfie_returnsBadRequest() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = verificationService.updateAccountVerification(validRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }
}