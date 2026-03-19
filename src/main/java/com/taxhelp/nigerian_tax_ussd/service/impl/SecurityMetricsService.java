package com.taxhelp.nigerian_tax_ussd.service.impl;

import com.taxhelp.nigerian_tax_ussd.model.request.*;
import com.taxhelp.nigerian_tax_ussd.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityMetricsService {

    private final SecurityEventRepository securityEventRepository;

    public Page<SecurityEventDto> getEvents(SecurityEventFilter filter, Pageable pageable) {
        LocalDateTime from = filter.getFrom() != null ? filter.getFrom().atStartOfDay() : null;
        LocalDateTime to   = filter.getTo()   != null ? filter.getTo().atTime(23, 59, 59) : null;

        return securityEventRepository
                .findByFilters(filter.getEventType(), filter.getSeverity(), from, to, pageable)
                .map(SecurityEventDto::from);
    }

    public SecuritySummaryDto buildSummary() {
        long total  = securityEventRepository.count();
        long recent = securityEventRepository.countByTimestampAfter(LocalDateTime.now().minusHours(24));

        Map<String, Long> byType     = toMap(securityEventRepository.countGroupByEventTypeRaw());
        Map<String, Long> bySeverity = toMap(securityEventRepository.countGroupBySeverityRaw());
        List<TopOffenderDto> offenders = getTopOffenders(5);

        return SecuritySummaryDto.builder()
                .totalEvents(total)
                .last24Hours(recent)
                .byType(byType)
                .bySeverity(bySeverity)
                .topOffenders(offenders)
                .build();
    }

    public List<RecentThreatDto> getRecentThreats(int limit) {
        return securityEventRepository
                .findTopNByOrderByTimestampDesc(PageRequest.of(0, limit))
                .stream()
                .map(RecentThreatDto::from)
                .collect(Collectors.toList());
    }

    public List<TopOffenderDto> getTopOffenders(int limit) {
        return securityEventRepository
                .findTopOffendersRaw(PageRequest.of(0, limit))
                .stream()
                .map(row -> TopOffenderDto.builder()
                        .phoneNumber((String) row[0])
                        .attempts((Long) row[1])
                        .lastAttempt((LocalDateTime) row[2])
                        .blocked((Long) row[1] >= 5)
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, Long> toMap(List<Object[]> raw) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : raw) {
            result.put(row[0] == null ? "UNKNOWN" : row[0].toString(),
                       row[1] == null ? 0L : (Long) row[1]);
        }
        return result;
    }
}