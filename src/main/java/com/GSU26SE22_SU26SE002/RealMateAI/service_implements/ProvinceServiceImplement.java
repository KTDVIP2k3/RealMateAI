package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Province;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ProvinceRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ProvinceDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.ProvinceServiceInterface;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProvinceServiceImplement implements ProvinceServiceInterface {
    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ResponseEntity<ApiResponse> getProvinces() {
       try{
           List<Province> provinceList= provinceRepository.findAll();
           if(provinceList.isEmpty()){
               return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(provinceList, "Province list is empty"));
           }
           List<ProvinceDTO> dtoList = provinceList.stream()
                   .map(province -> modelMapper.map(province, ProvinceDTO.class))
                   .collect(Collectors.toList());
           return ResponseEntity.status(HttpStatus.OK)
                   .body(ApiResponse.success(dtoList, "Get provinces successfully"));
       }catch (Exception e){
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
       }
    }
}
