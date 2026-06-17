package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Property;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyType;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PropertyTypeRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PropertyTypeDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PropertyTypeServiceInterface;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropertyTypeServiceImplement implements PropertyTypeServiceInterface {
    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ResponseEntity<ApiResponse> getPropertyTypes() {
        try{
            List<PropertyType> propertyTypes = propertyTypeRepository.findAll().stream().toList();

            if(propertyTypes.isEmpty()){
                return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(null, "List Property Type empty"));
            }

            List<PropertyTypeDTO> propertyTypeDTOS = propertyTypes.stream().map(propertyType -> modelMapper.map(propertyType, PropertyTypeDTO.class))
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(propertyTypeDTOS, "List Property Type"));
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}
