package com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import org.springframework.http.ResponseEntity;

public interface LocationFetchServiceInterface {
    ResponseEntity<ApiResponse> fetchAndSyncV2Data();
    ResponseEntity<ApiResponse> getWardsByProvince(String provinceCode);
    ResponseEntity<ApiResponse> fetchAndSyncSpecificProvince(String provinceCode);
    ResponseEntity<ApiResponse> getAllProvinces();
    ResponseEntity<ApiResponse> getProvinceDetail(String provinceCode);
    ResponseEntity<ApiResponse> deleteAllProvinces();
    ResponseEntity<ApiResponse> deleteAllWards();
    ResponseEntity<ApiResponse> deleteWardsByProvince(String provinceCode);
    ResponseEntity<ApiResponse> deleteSpecificProvince(String provinceCode);
}