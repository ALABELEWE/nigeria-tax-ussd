package com.taxhelp.nigerian_tax_ussd.model.request;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class UsageOverviewDto {
    private long totalSessions;
    private long activeSessions;
    private long totalQuestionsAsked;
    private double avgQuestionsPerSession;
    private long avgResponseTimeMs;
    private double successRate;
    private long dailyActiveUsers;
    private String peakHour;
    private Map<String, Long> sessionsByLanguage;
}