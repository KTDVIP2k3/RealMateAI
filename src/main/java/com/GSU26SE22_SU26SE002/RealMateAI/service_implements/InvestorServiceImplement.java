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

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(modelMapper.map(investor, InvestorDTO.class), "Investor Survey"));
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
//           investor.setLinkSocial(investorSurveyRequest.getLinkSocial());
//           investor.setAddress(investorSurveyRequest.getAddress());
           investor.setInvestmentStyle(investorSurveyRequest.getInvestmentStyle());
           investor.setInvestmentExperience(investorSurveyRequest.getInvestmentExperience());
           investor.setProfitTarget(investorSurveyRequest.getProfitTarget());
           investor.setManagementAbility(investorSurveyRequest.getManagementAbility());
           investor.setLevelOfVolatility(investorSurveyRequest.getLevelOfVolatility());
           investor.setCapitalUtilizationMindset(investorSurveyRequest.getCapitalUtilizationMindset());
           investor.setPositionalPriority(investorSurveyRequest.getPositionalPriority());
           investor.setInvestmentMethod(investorSurveyRequest.getInvestmentMethod());
           investor.setStableIncome(investorSurveyRequest.getStableIncome());
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
//           investor.setLinkSocial(investorSurveyRequest.getLinkSocial());
//           investor.setAddress(investorSurveyRequest.getAddress());
           investor.setInvestmentStyle(investorSurveyRequest.getInvestmentStyle());
           investor.setInvestmentExperience(investorSurveyRequest.getInvestmentExperience());
           investor.setProfitTarget(investorSurveyRequest.getProfitTarget());
           investor.setManagementAbility(investorSurveyRequest.getManagementAbility());
           investor.setLevelOfVolatility(investorSurveyRequest.getLevelOfVolatility());
           investor.setCapitalUtilizationMindset(investorSurveyRequest.getCapitalUtilizationMindset());
           investor.setPositionalPriority(investorSurveyRequest.getPositionalPriority());
           investor.setInvestmentMethod(investorSurveyRequest.getInvestmentMethod());
           investor.setStableIncome(investorSurveyRequest.getStableIncome());
           investor.setUpdatedAt(LocalDateTime.now());
           investor.setIsActive(true);
           investorRepository.save(investor);
           return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "Update investor survey successfully"));
       } catch (Exception e) {
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Sever_Erorr", e.getMessage()));
       }
    }
}
