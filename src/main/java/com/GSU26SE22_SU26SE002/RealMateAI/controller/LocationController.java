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

    @PostMapping("/catch-data-vietnam-province")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Đồng bộ toàn bộ dữ liệu Tỉnh/Xã sáp nhập từ API v2 (Chỉ dùng khi DB trống)")
    public ResponseEntity<ApiResponse> syncProvincesV2() {
        return locationFetchServiceInterface.fetchAndSyncV2Data();
    }

    @PostMapping("/catch-data-vietnam-province/{provinceCode}")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Đồng bộ dữ liệu của duy nhất một Tỉnh/Thành phố chỉ định từ API v2")
    public ResponseEntity<ApiResponse> syncSpecificProvinceV2(@PathVariable String provinceCode) {
        return locationFetchServiceInterface.fetchAndSyncSpecificProvince(provinceCode);
    }

    @GetMapping("/provinces")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Lấy danh sách tất cả các Tỉnh/Thành phố")
    public ResponseEntity<ApiResponse> getAllProvinces() {
        return locationFetchServiceInterface.getAllProvinces();
    }

    @GetMapping("/provinces/{provinceCode}")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Lấy thông tin chi tiết của một Tỉnh/Thành phố cụ thể")
    public ResponseEntity<ApiResponse> getProvinceDetail(@PathVariable String provinceCode) {
        return locationFetchServiceInterface.getProvinceDetail(provinceCode);
    }

    @GetMapping("/provinces/{provinceCode}/wards")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Lấy danh sách các Phường/Xã trực thuộc Tỉnh/Thành phố lựa chọn")
    public ResponseEntity<ApiResponse> getWardsByProvince(@PathVariable String provinceCode) {
        return locationFetchServiceInterface.getWardsByProvince(provinceCode);
    }

    @DeleteMapping("/provinces")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Xóa toàn bộ Tỉnh/Thành phố và tất cả Phường/Xã trong hệ thống")
    public ResponseEntity<ApiResponse> deleteAllProvinces() {
        return locationFetchServiceInterface.deleteAllProvinces();
    }

    @DeleteMapping("/wards")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Xóa toàn bộ dữ liệu Phường/Xã của tất cả các tỉnh")
    public ResponseEntity<ApiResponse> deleteAllWards() {
        return locationFetchServiceInterface.deleteAllWards();
    }

    @DeleteMapping("/provinces/{provinceCode}/wards")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db.  Xóa toàn bộ Phường/Xã của một Tỉnh/Thành phố cụ thể")
    public ResponseEntity<ApiResponse> deleteWardsByProvince(@PathVariable String provinceCode) {
        return locationFetchServiceInterface.deleteWardsByProvince(provinceCode);
    }

    @DeleteMapping("/provinces/{provinceCode}")
    @Operation(summary = "[Test Only]. Không đụng vào, chỉ test lấy data nạp vào db. Xoá thành phố cụ thể và các phường xã trực thuộc")
    public ResponseEntity<ApiResponse> deleteSpecificProvince(@PathVariable String provinceCode) {
        return locationFetchServiceInterface.deleteSpecificProvince(provinceCode);
    }
}