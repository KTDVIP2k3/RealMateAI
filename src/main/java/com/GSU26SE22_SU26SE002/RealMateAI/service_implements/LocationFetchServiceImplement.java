package com.GSU26SE22_SU26SE002.RealMateAI.service_implements;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Province;
import com.GSU26SE22_SU26SE002.RealMateAI.model.Ward;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.ProvinceRepository;
import com.GSU26SE22_SU26SE002.RealMateAI.responses.ApiResponse;
import com.GSU26SE22_SU26SE002.RealMateAI.service_interfaces.LocationFetchServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocationFetchServiceImplement implements LocationFetchServiceInterface {

    @Autowired
    private ProvinceRepository provinceRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final String API_V2_URL = "https://provinces.open-api.vn/api/v2/?depth=2";

    @Override
    public ResponseEntity<ApiResponse> fetchAndSyncV2Data() {
        try {
            List<String> existingProvinceCodes = provinceRepository.findAll().stream()
                    .map(Province::getProvince_code)
                    .collect(Collectors.toList());

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    API_V2_URL,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> provinceV2List = response.getBody();
            if (provinceV2List == null || provinceV2List.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "Không lấy được dữ liệu từ nguồn API v2 công khai."));
            }

            List<Province> provincesToSave = new ArrayList<>();
            int skippedCount = 0;

            for (Map<String, Object> pMap : provinceV2List) {
                String pCode = pMap.get("code") != null ? pMap.get("code").toString() : null;

                if (pCode != null && existingProvinceCodes.contains(pCode)) {
                    skippedCount++;
                    continue;
                }

                String pName = pMap.get("name") != null ? pMap.get("name").toString() : null;
                String pNameEn = pMap.get("name_en") != null ? pMap.get("name_en").toString() : null;
                String pFullName = pMap.get("full_name") != null ? pMap.get("full_name").toString() : pName;
                String pCodeName = pMap.get("code_name") != null ? pMap.get("code_name").toString() : null;

                Province province = Province.builder()
                        .province_code(pCode)
                        .name(pName)
                        .nameEn(pNameEn)
                        .fullName(pFullName)
                        .codeName(pCodeName)
                        .build();

                List<Ward> targetWards = new ArrayList<>();
                List<Map<String, Object>> wList = (List<Map<String, Object>>) pMap.get("wards");
                if (wList == null) {
                    wList = (List<Map<String, Object>>) pMap.get("ward");
                }

                if (wList != null) {
                    for (Map<String, Object> wMap : wList) {
                        String wCode = wMap.get("code") != null ? wMap.get("code").toString() : null;
                        String wName = wMap.get("name") != null ? wMap.get("name").toString() : null;
                        String wNameEn = wMap.get("name_en") != null ? wMap.get("name_en").toString() : null;
                        String wFullName = wMap.get("full_name") != null ? wMap.get("full_name").toString() : wName;
                        String wCodeName = wMap.get("code_name") != null ? wMap.get("code_name").toString() : null;

                        Ward ward = Ward.builder()
                                .ward_code(wCode)
                                .name(wName)
                                .nameEn(wNameEn)
                                .fullName(wFullName)
                                .codeName(wCodeName)
                                .province(province)
                                .build();
                        targetWards.add(ward);
                    }
                }
                province.setWards(targetWards);
                provincesToSave.add(province);
            }

            if (provincesToSave.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(
                        Map.of("addedProvinces", 0, "skippedProvinces", skippedCount),
                        "Tất cả các tỉnh thành đã tồn tại trong hệ thống. Không có dữ liệu mới được đồng bộ."
                ));
            }

            provinceRepository.saveAll(provincesToSave);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            Map.of("addedProvinces", provincesToSave.size(), "skippedProvinces", skippedCount),
                            "Đồng bộ bổ sung dữ liệu sáp nhập mới vào hệ thống thành công!"
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", "Lỗi đồng bộ: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getWardsByProvince(String provinceCode) {
        try {
            Province province = provinceRepository.findById(provinceCode).orElse(null);

            if (province == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Không tìm thấy Tỉnh/Thành phố với mã: " + provinceCode));
            }

            List<Map<String, Object>> wardList = province.getWards().stream()
                    .map(ward -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("ward_code", ward.getWard_code());
                        map.put("name", ward.getName());
                        map.put("nameEn", ward.getNameEn());
                        map.put("fullName", ward.getFullName());
                        map.put("codeName", ward.getCodeName());
                        return map;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("province_code", province.getProvince_code());
            responseData.put("name", province.getName());
            responseData.put("nameEn", province.getNameEn());
            responseData.put("fullName", province.getFullName());
            responseData.put("codeName", province.getCodeName());
            responseData.put("wards", wardList);

            return ResponseEntity.ok(ApiResponse.success(responseData, "Lấy danh sách Phường/Xã thành công"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    public ResponseEntity<ApiResponse> fetchAndSyncSpecificProvince(String provinceCode) {
        try {
            if (provinceRepository.existsById(provinceCode)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "Mã tỉnh thành này đã tồn tại trong hệ thống. Không thể đồng bộ lại!"));
            }

            String targetUrl = "https://provinces.open-api.vn/api/v2/p/" + provinceCode + "?depth=2";

            Map<String, Object> pMap;
            try {
                pMap = restTemplate.getForObject(targetUrl, Map.class);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Không thể tìm thấy hoặc kết nối tới mã tỉnh này trên API v2 công khai."));
            }

            if (pMap == null || pMap.get("code") == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail("Bad_Request", "Dữ liệu trả về từ API v2 không hợp lệ."));
            }

            String pCode = pMap.get("code").toString();
            String pName = pMap.get("name") != null ? pMap.get("name").toString() : null;
            String pNameEn = pMap.get("name_en") != null ? pMap.get("name_en").toString() : null;
            String pFullName = pMap.get("full_name") != null ? pMap.get("full_name").toString() : pName;
            String pCodeName = pMap.get("code_name") != null ? pMap.get("code_name").toString() : null;

            Province province = Province.builder()
                    .province_code(pCode)
                    .name(pName)
                    .nameEn(pNameEn)
                    .fullName(pFullName)
                    .codeName(pCodeName)
                    .build();

            List<Ward> targetWards = new ArrayList<>();
            List<Map<String, Object>> wList = (List<Map<String, Object>>) pMap.get("wards");
            if (wList == null) {
                wList = (List<Map<String, Object>>) pMap.get("ward");
            }

            if (wList != null) {
                for (Map<String, Object> wMap : wList) {
                    String wCode = wMap.get("code") != null ? wMap.get("code").toString() : null;
                    String wName = wMap.get("name") != null ? wMap.get("name").toString() : null;
                    String wNameEn = wMap.get("name_en") != null ? wMap.get("name_en").toString() : null;
                    String wFullName = wMap.get("full_name") != null ? wMap.get("full_name").toString() : wName;
                    String wCodeName = wMap.get("code_name") != null ? wMap.get("code_name").toString() : null;

                    Ward ward = Ward.builder()
                            .ward_code(wCode)
                            .name(wName)
                            .nameEn(wNameEn)
                            .fullName(wFullName)
                            .codeName(wCodeName)
                            .province(province)
                            .build();
                    targetWards.add(ward);
                }
            }
            province.setWards(targetWards);

            provinceRepository.save(province);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            Map.of("provinceCode", province.getProvince_code(), "totalWards", targetWards.size()),
                            "Đồng bộ thành phố " + province.getFullName() + " thành công!"
                    ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", "Lỗi đồng bộ thành phố: " + e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getAllProvinces() {
        try {
            List<Province> provinces = provinceRepository.findAll();

            List<Map<String, Object>> provinceList = provinces.stream()
                    .map(province -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("province_code", province.getProvince_code());
                        map.put("name", province.getName());
                        map.put("nameEn", province.getNameEn());
                        map.put("fullName", province.getFullName());
                        map.put("codeName", province.getCodeName());
                        return map;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(provinceList, "Lấy danh sách Tỉnh/Thành phố thành công"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse> getProvinceDetail(String provinceCode) {
        try {
            Province province = provinceRepository.findById(provinceCode).orElse(null);

            if (province == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Không tìm thấy Tỉnh/Thành phố với mã: " + provinceCode));
            }

            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("province_code", province.getProvince_code());
            responseData.put("name", province.getName());
            responseData.put("nameEn", province.getNameEn());
            responseData.put("fullName", province.getFullName());
            responseData.put("codeName", province.getCodeName());

            return ResponseEntity.ok(ApiResponse.success(responseData, "Lấy thông tin chi tiết Tỉnh/Thành phố thành công"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> deleteAllProvinces() {
        try {
            long count = provinceRepository.count();
            provinceRepository.deleteAll();
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("deletedProvincesCount", count),
                    "Xóa toàn bộ Tỉnh/Thành phố và các Phường/Xã liên quan thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> deleteAllWards() {
        try {
            List<Province> provinces = provinceRepository.findAll();
            long totalWardsDeleted = 0;
            for (Province province : provinces) {
                if (province.getWards() != null) {
                    totalWardsDeleted += province.getWards().size();
                    province.getWards().clear();
                }
            }
            provinceRepository.saveAll(provinces);
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("deletedWardsCount", totalWardsDeleted),
                    "Xóa toàn bộ Phường/Xã của tất cả các tỉnh thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> deleteWardsByProvince(String provinceCode) {
        try {
            Province province = provinceRepository.findById(provinceCode).orElse(null);
            if (province == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Không tìm thấy Tỉnh/Thành phố với mã: " + provinceCode));
            }

            int wardsCount = province.getWards() != null ? province.getWards().size() : 0;
            if (province.getWards() != null) {
                province.getWards().clear();
            }
            provinceRepository.save(province);

            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("provinceCode", provinceCode, "deletedWardsCount", wardsCount),
                    "Xóa toàn bộ Phường/Xã thuộc tỉnh thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<ApiResponse> deleteSpecificProvince(String provinceCode) {
        try {
            Province province = provinceRepository.findById(provinceCode).orElse(null);

            if (province == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.fail("Not_Found", "Không tìm thấy Tỉnh/Thành phố với mã: " + provinceCode));
            }

            int wardsCount = province.getWards() != null ? province.getWards().size() : 0;
            String provinceName = province.getFullName();

            provinceRepository.delete(province);

            return ResponseEntity.ok(ApiResponse.success(
                    Map.of(
                            "provinceCode", provinceCode,
                            "provinceName", provinceName,
                            "deletedWardsCount", wardsCount
                    ),
                    "Xóa thành phố " + provinceName + " và toàn bộ " + wardsCount + " Phường/Xã liên quan thành công!"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("Server_Error", "Lỗi khi xóa tỉnh thành: " + e.getMessage()));
        }
    }
}