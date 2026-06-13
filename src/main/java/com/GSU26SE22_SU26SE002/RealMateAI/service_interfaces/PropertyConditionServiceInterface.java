package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface PropertyConditionServiceInterface {
    public ResponseEntity<ApiResponse> getPropertyConditionByTypeId(Integer propertyTypeId);
}
