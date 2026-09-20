package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.enums.RoleEnum;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.CreateAccountRequestV2;
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

import java.util.List;

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
    @DisplayName("View My Account")
    class GetMyAccountTests {

        @Test
        @DisplayName("Valid account returns OK with profile")
        void getAccountProfile_valid_returnsOk() {
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = accountService.getAccountProfile();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Account Profile", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getAccountProfile_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = accountService.getAccountProfile();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @Nested
        @DisplayName("Create Account V2")
        class CreateAccountV2Tests {

            private CreateAccountRequestV2 validRequest;

            @BeforeEach
            void setupRequest() {
                validRequest = new CreateAccountRequestV2();
                validRequest.setUserName("newuser");
                validRequest.setPassword("Valid1@Pass");
                validRequest.setEmail("new@gmail.com");
                validRequest.setFullName("New User");
                validRequest.setRole(RoleEnum.Seller);
            }

            @Test
            @DisplayName("Valid account creation returns OK")
            void createAccount_valid_returnsOk() {
                when(accountRepository.findAll()).thenReturn(List.of());
                when(accountRepository.save(any(Account.class))).thenReturn(sampleAccount);

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals("Create account successfully", response.getBody().getMessage());
                verify(accountRepository).save(any(Account.class));
            }

            @Test
            @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
            void createAccount_exception_returnsServerError() {
                when(accountRepository.findAll()).thenThrow(new RuntimeException("DB error"));

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
                assertEquals("DB error", response.getBody().getMessage());
            }

            @Test
            @DisplayName("Existed username returns BAD_REQUEST")
            void createAccount_existedUsername_returnsBadRequest() {
                sampleAccount.setUserName("newuser");
                when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("User Name exists", response.getBody().getMessage());
            }

            @Test
            @DisplayName("Username containing space returns BAD_REQUEST")
            void createAccount_usernameWithSpace_returnsBadRequest() {
                validRequest.setUserName("new user");
                when(accountRepository.findAll()).thenReturn(List.of());

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("Tên đăng nhập không được chứa khoảng trắng", response.getBody().getMessage());
            }

            @Test
            @DisplayName("Username containing special chars returns BAD_REQUEST")
            void createAccount_usernameSpecialChars_returnsBadRequest() {
                validRequest.setUserName("newuser@!");
                when(accountRepository.findAll()).thenReturn(List.of());

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("Tên đăng nhập không được chứa dấu tiếng Việt hoặc ký tự đặc biệt", response.getBody().getMessage());
            }

            @Test
            @DisplayName("Username invalid length returns BAD_REQUEST")
            void createAccount_usernameInvalidLength_returnsBadRequest() {
                validRequest.setUserName("ab");
                when(accountRepository.findAll()).thenReturn(List.of());

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("Tên đăng nhập phải từ 3 đến 20 ký tự", response.getBody().getMessage());
            }

            @Test
            @DisplayName("Empty password returns BAD_REQUEST")
            void createAccount_emptyPassword_returnsBadRequest() {
                validRequest.setPassword("");
                when(accountRepository.findAll()).thenReturn(List.of());

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("Mật khẩu không được để trống", response.getBody().getMessage());
            }

            @Test
            @DisplayName("Password containing space returns BAD_REQUEST")
            void createAccount_passwordWithSpace_returnsBadRequest() {
                validRequest.setPassword("Valid 1@");
                when(accountRepository.findAll()).thenReturn(List.of());

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("Mật khẩu không được chứa khoảng trắng", response.getBody().getMessage());
            }

            @Test
            @DisplayName("Password invalid length returns BAD_REQUEST")
            void createAccount_passwordInvalidLength_returnsBadRequest() {
                validRequest.setPassword("Val1@");
                when(accountRepository.findAll()).thenReturn(List.of());

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("Mật khẩu phải từ 8 đến 32 ký tự", response.getBody().getMessage());
            }

            @Test
            @DisplayName("Password missing required format returns BAD_REQUEST")
            void createAccount_passwordInvalidFormat_returnsBadRequest() {
                validRequest.setPassword("password123");
                when(accountRepository.findAll()).thenReturn(List.of());

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("Mật khẩu phải bao gồm cả chữ hoa, chữ thường, số và ký tự đặc biệt", response.getBody().getMessage());
            }

            @Test
            @DisplayName("Existed email with same role returns BAD_REQUEST")
            void createAccount_existedEmailWithRole_returnsBadRequest() {
                sampleAccount.setEmail("new@gmail.com");
                sampleAccount.setRole(RoleEnum.Seller);
                when(accountRepository.findAll()).thenReturn(List.of(sampleAccount));

                ResponseEntity<ApiResponse> response = accountService.createAccount(validRequest);

                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
                assertEquals("Email này đã được đăng ký cho vai trò tương ứng", response.getBody().getMessage());
            }
        }

        @Test
        @DisplayName("Non-existent account returns NOT_FOUND")
        void getAccountProfile_accountNull_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = accountService.getAccountProfile();

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exist", response.getBody().getMessage());
        }
    }

    @Nested
    @DisplayName("Update My Account")
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
        @DisplayName("Valid update with new avatar returns OK")
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
        @DisplayName("Valid update without avatar (null) returns OK")
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
        @DisplayName("Non-existent account returns NOT_FOUND")
        void updateAccount_accountNull_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = accountService.updateAccount(validRequest);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Account does not exist with avatar returns NOT_FOUND")
        void updateAccount_accountNullWithAvatar_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = accountService.updateAccount(validRequest);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void updateAccount_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = accountService.updateAccount(validRequest);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("DB error", response.getBody().getMessage());
        }

        @ParameterizedTest
        @DisplayName("Blank fields return BAD_REQUEST")
        @CsvSource({
                "'', '0987654321'",
                "'Updated Name', ''"
        })
        void updateAccount_blankFields_returnsBadRequest(String fullName, String phone) {
            validRequest.setFullName(fullName);
            validRequest.setPhone(phone);

            ResponseEntity<ApiResponse> response = accountService.updateAccount(validRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Fields cannot be blank", response.getBody().getMessage());
        }
    }
}