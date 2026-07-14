package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.PropertyConditionServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/property-conditions")
public class PropertyConditionController {
    @Autowired
    private PropertyConditionServiceInterface propertyConditionServiceInterface;

    @GetMapping
    public ResponseEntity<ApiResponse> getPropertyConditionsByTypeId(@RequestParam(name = "propertyTypeId") Integer propertyTypeId) {
        return propertyConditionServiceInterface.getPropertyConditionByTypeId(propertyTypeId);
    }
}