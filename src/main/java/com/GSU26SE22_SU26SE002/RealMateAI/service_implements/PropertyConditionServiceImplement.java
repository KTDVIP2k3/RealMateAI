package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyCondition;
import com.GSU26SE22_SU26SE002.RealMateAI.model.PropertyType;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PropertyConditionRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.PropertyTypeRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.PropertyConditionDTO;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PropertyConditionServiceInterface;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PropertyConditionServiceImplement implements PropertyConditionServiceInterface {
    @Autowired
    private PropertyConditionRepository propertyConditionRepository;

    @Autowired
    private PropertyTypeRepository propertyTypeRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ResponseEntity<ApiResponse> getPropertyConditionByTypeId(Integer propertyTypeId) {
        try {
            PropertyType propertyType = propertyTypeRepository.findById(propertyTypeId).orElse(null);
            if (propertyType == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Property type id does not exist"));
            }

            List<PropertyCondition> conditions = propertyConditionRepository.findAll().stream()
                    .filter(condition -> condition.getPropertyType().getPropertyTypeId().equals(propertyTypeId))
                    .collect(Collectors.toList());

            if (conditions.isEmpty()) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(ApiResponse.success(null, "List is empty"));
            }

            List<PropertyConditionDTO> conditionDTOS = conditions.stream()
                    .map(condition -> modelMapper.map(condition, PropertyConditionDTO.class))
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.OK)
                    .body(ApiResponse.success(conditionDTOS, "List property condition"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }
}
