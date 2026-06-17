package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Province;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Ward;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ProvinceRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.WardRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.WardDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.WardServiceInterface;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WardServiceImplement implements WardServiceInterface {
    @Autowired
    private WardRepository wardRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ProvinceRepository provinceRepository;

    @Override
    public ResponseEntity<ApiResponse> getWardListByProvince(String province_code) {
     try{
         Province province = provinceRepository.findById(province_code).orElse(null);
         if(province == null){
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.fail("Not_Found", "Province code does not exist"));
         }
         List<Ward> wards = wardRepository.findAll().stream()
                 .filter(ward -> ward.getProvince().getProvince_code().equalsIgnoreCase(province_code))
                 .collect(Collectors.toList());

         if(wards.isEmpty()){
             return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "List is empty"));
         }
         List<WardDTO> wardDTOS = wards.stream().map(ward -> modelMapper.map(ward, WardDTO.class)).collect(Collectors.toList());
         return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(wardDTOS, "List ward"));
     } catch (Exception e) {
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
     }
    }
}
