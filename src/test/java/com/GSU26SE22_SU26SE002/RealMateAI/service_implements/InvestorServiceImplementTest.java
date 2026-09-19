package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Investor;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.InvestorRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestorSurveyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestorServiceImplement - Investor Survey")
class InvestorServiceImplementTest {

    @Mock
    private InvestorRepository investorRepository;
    
    @Mock
    private AuthenUntil authenUntil;
    
    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private InvestorServiceImplement investorService;

    private Account sampleAccount;
    private Investor sampleInvestor;
    private InvestorSurveyRequest request;

    @BeforeEach
    void setUp() {
        sampleAccount = new Account();
        sampleAccount.setAccountId(1);

        sampleInvestor = new Investor();
        sampleInvestor.setInvestorId(1);
        
        request = new InvestorSurveyRequest();
        request.setInvestmentExperience("Beginner");
        request.setStableIncome(true);
        request.setInvestmentGoal("Long term");
        request.setInvestmentPriority("Safety");
        request.setInvestmentStyle("Conservative");
        request.setReturnExpectation("Medium");
        request.setPropertyPreference("Apartment");
        request.setDecisionFactor("Price");
        request.setManagementAbility("Low");
        request.setInvestmentMethod("Direct");
    }

    @Nested
    @DisplayName("F-057. View Survey")
    class ViewSurveyTests {

        @Test
        @DisplayName("Valid request returns OK")
        void getInvestorSurvey_valid_returnsOk() {
            sampleAccount.setInvestor(sampleInvestor);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(investorRepository.findById(1)).thenReturn(Optional.of(sampleInvestor));

            ResponseEntity<ApiResponse> response = investorService.getInvestorSurvey();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Investor Survey", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Account has no investor returns INTERNAL_SERVER_ERROR")
        void getInvestorSurvey_noInvestor_returnsServerError() {
            sampleAccount.setInvestor(null);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investorService.getInvestorSurvey();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("Investor of this account does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Investor not found in DB returns NOT_FOUND")
        void getInvestorSurvey_notFound_returnsNotFound() {
            sampleAccount.setInvestor(sampleInvestor);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(investorRepository.findById(1)).thenReturn(Optional.empty());

            ResponseEntity<ApiResponse> response = investorService.getInvestorSurvey();

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Investor does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void getInvestorSurvey_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investorService.getInvestorSurvey();

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("F-058. Submit Survey")
    class SubmitSurveyTests {

        @Test
        @DisplayName("Valid request returns OK")
        void createInvestorSurvey_valid_returnsOk() {
            sampleAccount.setInvestor(null);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(investorRepository.save(any())).thenReturn(sampleInvestor);

            ResponseEntity<ApiResponse> response = investorService.createInvestorSurvey(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Create investor survey successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Account does not exist returns NOT_FOUND")
        void createInvestorSurvey_noAccount_returnsNotFound() {
            when(authenUntil.getCurrentUSer()).thenReturn(null);

            ResponseEntity<ApiResponse> response = investorService.createInvestorSurvey(request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Account doest not exist", response.getBody().getMessage()); // Typo in code
        }

        @Test
        @DisplayName("Account already has investor returns BAD_REQUEST")
        void createInvestorSurvey_hasInvestor_returnsBadRequest() {
            sampleAccount.setInvestor(sampleInvestor);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investorService.createInvestorSurvey(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("This account has investor so just update investor", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void createInvestorSurvey_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investorService.createInvestorSurvey(request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("F-059. Update Survey")
    class UpdateSurveyTests {

        @Test
        @DisplayName("Valid request returns OK")
        void updateInvestorSurvey_valid_returnsOk() {
            sampleAccount.setInvestor(sampleInvestor);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);
            when(investorRepository.save(any())).thenReturn(sampleInvestor);

            ResponseEntity<ApiResponse> response = investorService.updateInvestorSurvey(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("Update investor survey successfully", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Account has no investor returns NOT_FOUND")
        void updateInvestorSurvey_noInvestor_returnsNotFound() {
            sampleAccount.setInvestor(null);
            when(authenUntil.getCurrentUSer()).thenReturn(sampleAccount);

            ResponseEntity<ApiResponse> response = investorService.updateInvestorSurvey(request);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertEquals("Investor does not exist", response.getBody().getMessage());
        }

        @Test
        @DisplayName("Exception returns INTERNAL_SERVER_ERROR")
        void updateInvestorSurvey_exception_returnsServerError() {
            when(authenUntil.getCurrentUSer()).thenThrow(new RuntimeException("DB error"));

            ResponseEntity<ApiResponse> response = investorService.updateInvestorSurvey(request);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }
}