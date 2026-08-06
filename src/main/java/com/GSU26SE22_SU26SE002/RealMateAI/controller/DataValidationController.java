package com.GSU26SE22_SU26SE002.RealMateAI.controllers;

import com.GSU26SE22_SU26SE002.RealMateAI.service_implements.DataValidationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-data")
@Profile("local")
public class DataValidationController {

    @Autowired(required = false)
    private DataValidationService dataValidationService;

    @Operation(summary = "Test-Only. Trên local")
    @PostMapping("/run")
    public ResponseEntity<String> runAudit() {
        if (dataValidationService == null) {
            return ResponseEntity.badRequest().body("Audit Service chỉ chạy ở Profile 'local'!");
        }

        dataValidationService.startAuditProcess();

        return ResponseEntity.ok("🚀 Đã kích hoạt tiến trình Audit!");
    }
}