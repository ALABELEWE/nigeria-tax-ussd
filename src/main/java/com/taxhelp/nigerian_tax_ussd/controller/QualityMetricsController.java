package com.taxhelp.nigerian_tax_ussd.controller;

import com.taxhelp.nigerian_tax_ussd.common.ApiResponse;
import com.taxhelp.nigerian_tax_ussd.model.request.LowScoreQuestionDto;
import com.taxhelp.nigerian_tax_ussd.model.request.QualityMetricsDto;
import com.taxhelp.nigerian_tax_ussd.service.impl.QualityMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/quality")
@RequiredArgsConstructor
public class QualityMetricsController {

    private final QualityMetricsService qualityMetricsService;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<QualityMetricsDto>> getMetrics() {
        return ResponseEntity.ok(ApiResponse.success(qualityMetricsService.getMetrics()));
    }

    @GetMapping("/low-scores")
    public ResponseEntity<ApiResponse<Page<LowScoreQuestionDto>>> getLowScores(
            @RequestParam(defaultValue = "0.5") double threshold,
            @RequestParam(defaultValue = "20")  int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                qualityMetricsService.getLowScores(threshold, limit)));
    }
}