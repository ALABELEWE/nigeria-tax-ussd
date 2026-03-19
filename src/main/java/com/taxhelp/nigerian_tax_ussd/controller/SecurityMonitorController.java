package com.taxhelp.nigerian_tax_ussd.controller;

import com.taxhelp.nigerian_tax_ussd.common.ApiResponse;
import com.taxhelp.nigerian_tax_ussd.model.request.*;
import com.taxhelp.nigerian_tax_ussd.service.impl.SecurityMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/security")
@RequiredArgsConstructor
public class SecurityMonitorController {

    private final SecurityMetricsService securityMetricsService;

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<Page<SecurityEventDto>>> getSecurityEvents(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        SecurityEventFilter filter = SecurityEventFilter.builder()
                .eventType(eventType).severity(severity).from(from).to(to).build();
        return ResponseEntity.ok(ApiResponse.success(
                securityMetricsService.getEvents(filter, PageRequest.of(page, size))));
    }

    @GetMapping("/events/summary")
    public ResponseEntity<ApiResponse<SecuritySummaryDto>> getSecuritySummary() {
        return ResponseEntity.ok(ApiResponse.success(securityMetricsService.buildSummary()));
    }

    @GetMapping("/threats/recent")
    public ResponseEntity<ApiResponse<List<RecentThreatDto>>> getRecentThreats(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success(securityMetricsService.getRecentThreats(limit)));
    }

    @GetMapping("/offenders/top")
    public ResponseEntity<ApiResponse<List<TopOffenderDto>>> getTopOffenders(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.success(securityMetricsService.getTopOffenders(limit)));
    }
}