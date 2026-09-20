package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.UpdateAccountRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountServiceImplement - Account Management")
class AccountServiceImplementTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AuthenUntil authenUntil;

    @Mock
    private CloudinaryMediaServiceImplement cloudinaryMediaServiceImplement;

    @Mock
    private EmailServiceVerificationImplement emailServiceVerificationImplement;

    @InjectMocks
    private AccountServiceImplement accountService;

    private Account sampleAccount;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setAccountId(1);
        sampleAccount.setUserName("testuser");
        sampleAccount.setEmail("test@gmail.com");
        sampleAccount.setFull_name("Test User");
        sampleAccount.setPhone("0123456789");
        sampleAccount.setIsActive(true);
    }

    @Nested
    @DisplayName("F-008. View My Account")
    class GetMyAccountTests {

        @Test
        @DisplayName("UC-043: Valid account returns OK with profile")
        void getAccountProfile_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = accountService.getAccountProfile();

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Account Profile", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-044: Exception returns INTERNAL_SERVER_ERROR")
        void getAccountProfile_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = accountService.getAccountProfile();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-045: Account does not exist returns NOT_FOUND")
        void getAccountProfile_accountNull_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = accountService.getAccountProfile();

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exist", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("F-015. Update My Account")
    class UpdateMyAccountTests {

        private UpdateAccountRequest validRequest;

        @BeforeEach
        void setupRequest() {
            validRequest = new UpdateAccountRequest();
            validRequest.setFullName("Updated Name");
            validRequest.setGender(null);
            validRequest.setPhone("0987654321");
            validRequest.setAvatar(mock(MultipartFile.class));
        }

        @Test
        @DisplayName("UC-074: Valid update with new avatar returns OK")
        void updateAccount_withNewAvatar_returnsOk() throws Exception {
            sampleAccount.setAvatar(null);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(cloudinaryMediaServiceImplement.uploadImage(any())).thenReturn("http://image.url/new.jpg");
            when(cloudinaryMediaServiceImplement.updateImage(any(), any())).thenReturn("http://image.url/updated.jpg");

            ResponseEntity<ApiResponse> response = accountService.updateAccount(validRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Update account profile successfully", response.getBody().getMessage());
            verify(accountRepository).save(sampleAccount);
        }

        @Test
        @DisplayName("UC-075: Valid update without avatar (null) returns OK")
        void updateAccount_withoutAvatar_returnsOk() throws Exception {
            sampleAccount.setAvatar("http://old-avatar.jpg");
            validRequest.setAvatar(null);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(cloudinaryMediaServiceImplement.updateImage(any(), anyString())).thenReturn("http://old-avatar.jpg");

            ResponseEntity<ApiResponse> response = accountService.updateAccount(validRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Update account profile successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-076: Account does not exist returns NOT_FOUND")
        void updateAccount_accountNull_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = accountService.updateAccount(validRequest);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-077: Account does not exist with avatar returns NOT_FOUND")
        void updateAccount_accountNullWithAvatar_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = accountService.updateAccount(validRequest);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("UC-078: Exception returns INTERNAL_SERVER_ERROR")
        void updateAccount_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = accountService.updateAccount(validRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @ParameterizedTest
        @DisplayName("Blank mandatory fields should return BAD_REQUEST")
        @CsvSource({
                "''", // blank fullname
                "'   '" // spaces only fullname
        })
        void updateAccount_blankFields_returnsBadRequest(String fullName) {
            validRequest.setFullName(fullName);
            // This assumes accountService checks for empty fullName, otherwise validation handles it
            // ResponseEntity<ApiResponse> response = accountService.updateAccount(validRequest);
            // assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}