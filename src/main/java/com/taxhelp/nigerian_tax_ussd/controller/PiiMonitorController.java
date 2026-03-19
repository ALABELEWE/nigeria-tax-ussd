package com.taxhelp.nigerian_tax_ussd.controller;

import com.taxhelp.nigerian_tax_ussd.common.ApiResponse;
import com.taxhelp.nigerian_tax_ussd.model.request.PiiIncidentDto;
import com.taxhelp.nigerian_tax_ussd.model.request.PiiSummaryDto;
import com.taxhelp.nigerian_tax_ussd.service.impl.PiiMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/pii")
@RequiredArgsConstructor
public class PiiMonitorController {

    private final PiiMetricsService piiMetricsService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PiiSummaryDto>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(piiMetricsService.getSummary()));
    }

    @GetMapping("/incidents")
    public ResponseEntity<ApiResponse<List<PiiIncidentDto>>> getIncidents(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(piiMetricsService.getRecentIncidents(limit)));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.success(piiMetricsService.getTrend(days)));
    }
}