package com.taxhelp.nigerian_tax_ussd.controller;

import com.taxhelp.nigerian_tax_ussd.common.ApiResponse;
import com.taxhelp.nigerian_tax_ussd.model.request.DailyStatDto;
import com.taxhelp.nigerian_tax_ussd.model.request.HourlyStatDto;
import com.taxhelp.nigerian_tax_ussd.model.request.UsageOverviewDto;
import com.taxhelp.nigerian_tax_ussd.service.impl.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<UsageOverviewDto>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getOverview()));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<DailyStatDto>>> getDailyStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(14);
        if (to   == null) to   = LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDailyStats(from, to)));
    }

    @GetMapping("/hourly")
    public ResponseEntity<ApiResponse<List<HourlyStatDto>>> getHourlyStats() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getHourlyStats()));
    }
}