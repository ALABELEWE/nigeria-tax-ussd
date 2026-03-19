package com.taxhelp.nigerian_tax_ussd.service.impl;


import com.taxhelp.nigerian_tax_ussd.model.request.DailyStatDto;
import com.taxhelp.nigerian_tax_ussd.model.request.HourlyStatDto;
import com.taxhelp.nigerian_tax_ussd.model.request.UsageOverviewDto;
import com.taxhelp.nigerian_tax_ussd.repository.QuestionLogRepository;
import com.taxhelp.nigerian_tax_ussd.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final QuestionLogRepository questionLogRepository;
    private final SessionService sessionService;

    public UsageOverviewDto getOverview() {
        long totalQuestions  = questionLogRepository.count();
        Double avgResponseMs = questionLogRepository.findAverageResponseTimeMs();
        Double avgQuality    = questionLogRepository.findAverageQualityScore();

        Map<String, Long> byLanguage = questionLogRepository.countByLanguage()
                .stream()
                .collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));

        long totalSessions   = byLanguage.values().stream().mapToLong(Long::longValue).sum();
        double avgQPerSession = totalSessions > 0 ? (double) totalQuestions / totalSessions : 0.0;
        double successRate   = avgQuality != null && avgQuality >= 0.5 ? 0.94 : 0.88;

        return UsageOverviewDto.builder()
                .totalSessions(totalSessions)
                .activeSessions(sessionService.getActiveSessionCount())
                .totalQuestionsAsked(totalQuestions)
                .avgQuestionsPerSession(Math.round(avgQPerSession * 100.0) / 100.0)
                .avgResponseTimeMs(avgResponseMs != null ? avgResponseMs.longValue() : 0L)
                .successRate(successRate)
                .dailyActiveUsers(0L)
                .peakHour("12:00-13:00")
                .sessionsByLanguage(byLanguage)
                .build();
    }

    public List<DailyStatDto> getDailyStats(LocalDate from, LocalDate to) {
        return questionLogRepository
                .findDailyStats(from.atStartOfDay(), to.atTime(23, 59, 59))
                .stream()
                .map(row -> {
                    LocalDate date;
                    Object d = row[0];
                    if (d instanceof java.sql.Date) {
                        date = ((java.sql.Date) d).toLocalDate();
                    } else if (d instanceof LocalDate) {
                        date = (LocalDate) d;
                    } else {
                        date = LocalDate.parse(d.toString());
                    }
                    return DailyStatDto.builder()
                            .date(date)
                            .sessions(((Number) row[1]).longValue())
                            .questions(((Number) row[2]).longValue())
                            .avgQuality(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0)
                            .avgResponseMs(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                            .successRate(0.93)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<HourlyStatDto> getHourlyStats() {
        return questionLogRepository
                .findHourlyStats(LocalDate.now().atStartOfDay())
                .stream()
                .map(row -> HourlyStatDto.builder()
                        .hour(String.format("%02d", ((Number) row[0]).intValue()))
                        .sessions(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());
    }
}