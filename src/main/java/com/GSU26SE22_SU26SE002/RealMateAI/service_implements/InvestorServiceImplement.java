package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Investor;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.InvestorRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.requests.InvestorSurveyRequest;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.InvestorDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.InvestorServiceInterface;
import com.GSU26SE22_SU26SE002.RealMateAI.utils.AuthenUntil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvestorServiceImplement implements InvestorServiceInterface {
    @Autowired
    private InvestorRepository investorRepository;

    @Autowired
    private AuthenUntil authenUntil;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ResponseEntity<ApiResponse> getInvestorSurvey() {
        try{
            Account account = authenUntil.getCurrentUSer();
            if(account.getInvestor() == null){
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Sever_Error", "Investor of this account does not exist"));
            }
            Investor investor = investorRepository.findById(account.getInvestor().getInvestorId()).orElse(null);
            if(investor == null){
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "Investor does not exist"));
            }

            InvestorDTO investorDTO = new InvestorDTO();
            investorDTO.setSurveyId(investor.getInvestorId());
            investorDTO.setInvestmentExperience(investor.getInvestmentExperience());
            investorDTO.setStableIncome(investor.getStableIncome());
            investorDTO.setInvestmentGoal(investor.getInvestmentGoal());
            investorDTO.setInvestmentPriority(investor.getInvestmentPriority());
            investorDTO.setInvestmentStyle(investor.getInvestmentStyle());
            investorDTO.setReturnExpectation(investor.getReturnExpectation());
            investorDTO.setPropertyPreference(investor.getPropertyPreference());
            investorDTO.setDecisionFactor(investor.getDecisionFactor());
            investorDTO.setManagementAbility(investor.getManagementAbility());
            investorDTO.setInvestmentMethod(investor.getInvestmentMethod());
            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(investorDTO, "Investor Survey"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Sever_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> createInvestorSurvey(InvestorSurveyRequest investorSurveyRequest) {
       try{
           Account account = authenUntil.getCurrentUSer();
           if(account == null){
               return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "Account doest not exist"));
           }

           if(account.getInvestor() !=null){
               return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.fail("Bad_Request", "This account has investor so just update investor"));
           }

           Investor investor = new Investor();
           investor.setAccount(account);
           investor.setInvestmentExperience(investorSurveyRequest.getInvestmentExperience());
           investor.setStableIncome(investorSurveyRequest.getStableIncome());
           investor.setInvestmentGoal(investorSurveyRequest.getInvestmentGoal());
           investor.setInvestmentPriority(investorSurveyRequest.getInvestmentPriority());
           investor.setInvestmentStyle(investorSurveyRequest.getInvestmentStyle());
           investor.setReturnExpectation(investorSurveyRequest.getReturnExpectation());
           investor.setPropertyPreference(investorSurveyRequest.getPropertyPreference());
           investor.setDecisionFactor(investorSurveyRequest.getDecisionFactor());
           investor.setManagementAbility(investorSurveyRequest.getManagementAbility());
           investor.setInvestmentMethod(investorSurveyRequest.getInvestmentMethod());
           investor.setCreatedAt(LocalDateTime.now());
           investor.setIsActive(true);
           investorRepository.save(investor);
           return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Create investor survey successfully"));
       } catch (Exception e) {
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Sever_Errpr", e.getMessage()));
       }
    }

    @Override
    public ResponseEntity<ApiResponse> updateInvestorSurvey(InvestorSurveyRequest investorSurveyRequest) {
       try{
           Account account = authenUntil.getCurrentUSer();

           Investor investor = account.getInvestor();
           if(investor == null){
               return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "Investor does not exist"));
           }
           investor.setInvestmentExperience(investorSurveyRequest.getInvestmentExperience());
           investor.setStableIncome(investorSurveyRequest.getStableIncome());
           investor.setInvestmentGoal(investorSurveyRequest.getInvestmentGoal());
           investor.setInvestmentPriority(investorSurveyRequest.getInvestmentPriority());
           investor.setInvestmentStyle(investorSurveyRequest.getInvestmentStyle());
           investor.setReturnExpectation(investorSurveyRequest.getReturnExpectation());
           investor.setPropertyPreference(investorSurveyRequest.getPropertyPreference());
           investor.setDecisionFactor(investorSurveyRequest.getDecisionFactor());
           investor.setManagementAbility(investorSurveyRequest.getManagementAbility());
           investor.setInvestmentMethod(investorSurveyRequest.getInvestmentMethod());
           investor.setUpdatedAt(LocalDateTime.now());
           investor.setIsActive(true);
           investorRepository.save(investor);           return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Update investor survey successfully"));
       } catch (Exception e) {
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Sever_Erorr", e.getMessage()));
       }
    }
}
