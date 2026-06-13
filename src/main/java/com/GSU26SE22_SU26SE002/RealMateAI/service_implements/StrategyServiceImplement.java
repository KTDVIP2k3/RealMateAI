package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Strategy;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.StrategyRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.StrategyDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.StrategyServiceInterface;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StrategyServiceImplement implements StrategyServiceInterface {
    @Autowired
    private StrategyRepository strategyRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ResponseEntity<ApiResponse> getAllStrategies() {
        try {
            List<Strategy> strategyList = strategyRepository.findAll();
            if (strategyList.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(strategyList, "Strategy list is empty"));
            }
            List<StrategyDTO> dtoList = strategyList.stream()
                    .map(strategy -> modelMapper.map(strategy, StrategyDTO.class))
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(dtoList, "Get strategies successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}
