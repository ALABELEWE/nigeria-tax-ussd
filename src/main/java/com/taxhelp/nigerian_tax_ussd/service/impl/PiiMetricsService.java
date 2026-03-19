package com.taxhelp.nigerian_tax_ussd.service.impl;


import com.taxhelp.nigerian_tax_ussd.model.request.PiiIncidentDto;
import com.taxhelp.nigerian_tax_ussd.model.request.PiiSummaryDto;
import com.taxhelp.nigerian_tax_ussd.repository.QuestionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PiiMetricsService {

    private final QuestionLogRepository questionLogRepository;

    public PiiSummaryDto getSummary() {
        long total   = questionLogRepository.countByContainsPiiTrue();
        long last24h = questionLogRepository.countByContainsPiiTrueAndTimestampAfter(
                LocalDateTime.now().minusHours(24));
        long all     = questionLogRepository.count();

        Map<String, Long> byType = new LinkedHashMap<>();
        byType.put("PHONE_NUMBER", questionLogRepository.countPiiByType("PHONE"));
        byType.put("TIN",          questionLogRepository.countPiiByType("TIN"));
        byType.put("BVN",          questionLogRepository.countPiiByType("BVN"));
        byType.put("EMAIL",        questionLogRepository.countPiiByType("EMAIL"));

        return PiiSummaryDto.builder()
                .totalIncidents(total)
                .last24Hours(last24h)
                .redactionRate(1.0)
                .byType(byType)
                .incidentRate(all > 0 ? Math.round((double) total / all * 10000.0) / 100.0 : 0.0)
                .build();
    }

    public List<PiiIncidentDto> getRecentIncidents(int limit) {
        return questionLogRepository
                .findByContainsPiiTrueOrderByTimestampDesc(PageRequest.of(0, limit))
                .stream()
                .map(PiiIncidentDto::from)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTrend(int days) {
        return questionLogRepository
                .findDailyPiiCounts(LocalDateTime.now().minusDays(days))
                .stream()
                .map(row -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("date",  row[0].toString());
                    entry.put("count", row[1]);
                    return entry;
                })
                .collect(Collectors.toList());
    }
}