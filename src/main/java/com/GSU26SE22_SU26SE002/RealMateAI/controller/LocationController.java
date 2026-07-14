package com.GSU26SE22_SU26SE002.RealMateAI.controller;

import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.LocationFetchServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/locations")
public class LocationController {

    @Autowired
    private LocationFetchServiceInterface locationFetchServiceInterface;

    @PostMapping("/sync")
    @Operation(summary = "[Test Only]. Đồng bộ toàn bộ dữ liệu Tỉnh/Xã")
    public ResponseEntity<ApiResponse> syncProvincesV2() {
        return locationFetchServiceInterface.fetchAndSyncV2Data();
    }

    @PostMapping("/sync/{provinceCode}")
    @Operation(summary = "[Test Only]. Đồng bộ dữ liệu của duy nhất một Tỉnh/Thành phố")
    public ResponseEntity<ApiResponse> syncSpecificProvinceV2(@PathVariable("provinceCode") String provinceCode) {
        return locationFetchServiceInterface.fetchAndSyncSpecificProvince(provinceCode);
    }

    @GetMapping("/provinces")
    @Operation(summary = "[Test Only]. Lấy danh sách tất cả các Tỉnh/Thành phố")
    public ResponseEntity<ApiResponse> getAllProvinces() {
        return locationFetchServiceInterface.getAllProvinces();
    }

    @GetMapping("/provinces/{provinceCode}")
    @Operation(summary = "[Test Only]. Lấy thông tin chi tiết của một Tỉnh/Thành phố cụ thể")
    public ResponseEntity<ApiResponse> getProvinceDetail(@PathVariable("provinceCode") String provinceCode) {
        return locationFetchServiceInterface.getProvinceDetail(provinceCode);
    }

    @GetMapping("/provinces/{provinceCode}/wards")
    @Operation(summary = "[Test Only]. Lấy danh sách các Phường/Xã trực thuộc")
    public ResponseEntity<ApiResponse> getWardsByProvince(@PathVariable("provinceCode") String provinceCode) {
        return locationFetchServiceInterface.getWardsByProvince(provinceCode);
    }

    @DeleteMapping("/provinces")
    @Operation(summary = "[Test Only]. Xóa toàn bộ Tỉnh/Thành phố")
    public ResponseEntity<ApiResponse> deleteAllProvinces() {
        return locationFetchServiceInterface.deleteAllProvinces();
    }

    @DeleteMapping("/wards")
    @Operation(summary = "[Test Only]. Xóa toàn bộ dữ liệu Phường/Xã")
    public ResponseEntity<ApiResponse> deleteAllWards() {
        return locationFetchServiceInterface.deleteAllWards();
    }

    @DeleteMapping("/provinces/{provinceCode}/wards")
    @Operation(summary = "[Test Only]. Xóa toàn bộ Phường/Xã của một Tỉnh/Thành phố")
    public ResponseEntity<ApiResponse> deleteWardsByProvince(@PathVariable("provinceCode") String provinceCode) {
        return locationFetchServiceInterface.deleteWardsByProvince(provinceCode);
    }

    @DeleteMapping("/provinces/{provinceCode}")
    @Operation(summary = "[Test Only]. Xoá thành phố cụ thể và các phường xã trực thuộc")
    public ResponseEntity<ApiResponse> deleteSpecificProvince(@PathVariable("provinceCode") String provinceCode) {
        return locationFetchServiceInterface.deleteSpecificProvince(provinceCode);
    }
}