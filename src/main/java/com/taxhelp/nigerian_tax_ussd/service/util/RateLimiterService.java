package com.taxhelp.nigerian_tax_ussd.service.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class RateLimiterService {

    // Store: "phoneNumber_date" -> count
    private final Map<String, AtomicInteger> dailyRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastCleanupTime = new ConcurrentHashMap<>();

    // Configuration
    private static final int MAX_REQUESTS_PER_DAY = 50;
    private static final int MAX_REQUESTS_PER_HOUR = 15;

    // Store hourly counts: "phoneNumber_date_hour" -> count
    private final Map<String, AtomicInteger> hourlyRequestCounts = new ConcurrentHashMap<>();

    /**
     * Check if the user has exceeded their rate limit
     * @param phoneNumber User's phone number
     * @return true if within limit, false if exceeded
     */
    public boolean allowRequest(String phoneNumber) {
        cleanupOldEntries();

        // Check daily limimt
        if (!checkDailyLimit(phoneNumber)){
            log.warn("Daily rate limit exceeded for: {}", phoneNumber);
            return false;
        }

        // Check hourly limit
        if (!checkHourlyLimit(phoneNumber)){
            log.warn("Hourly rate limit exceeded for: {}", phoneNumber);
            return false;
        }

        // Increment counters
        incrementCounters(phoneNumber);

        log.info("Rate limit check passed for: {} (Daily: {}/{})",
                phoneNumber,
                getDailyCount(phoneNumber),
                MAX_REQUESTS_PER_DAY);

        return true;
    }

    /**
     * Get remaining requests for a phone number today
     */
    public int getRemainingRequests(String phoneNumber) {
        int dailyCount = getDailyCount(phoneNumber);
        return Math.max(0, MAX_REQUESTS_PER_DAY - dailyCount);
    }


    private String getHourlyKey(String phoneNumber) {
        LocalDateTime now = LocalDateTime.now();
        return phoneNumber + "_" + now.toLocalDate() + "_" + now.getHour();
    }

    private String getDailyKey(String phoneNumber) {
        return phoneNumber + "_" + LocalDate.now();
    }

    private boolean checkHourlyLimit(String phoneNumber) {
        int count = getHourlyCount(phoneNumber);
        return count < MAX_REQUESTS_PER_HOUR;
    }

    private int getHourlyCount(String phoneNumber) {
        String hourlyKey = getHourlyKey(phoneNumber);
        AtomicInteger count = hourlyRequestCounts.get(hourlyKey);
        return count != null ? count.get() : 0;
    }

    private boolean checkDailyLimit(String phoneNumber) {
        int count =  getDailyCount(phoneNumber);
        return count < MAX_REQUESTS_PER_DAY;
    }

    private int getDailyCount(String phoneNumber) {
        String dailyKey = getDailyKey(phoneNumber);
        AtomicInteger count = dailyRequestCounts.get(dailyKey);
        return count != null ? count.get() : 0;
    }

    private void incrementCounters(String phoneNumber) {
        // Increment daily counter
        String dailyKey = getDailyKey(phoneNumber);
        dailyRequestCounts.computeIfAbsent(dailyKey, k -> new AtomicInteger(0))
                .incrementAndGet();

        // Increment hourly counter
        String hourlyKey = getHourlyKey(phoneNumber);
        hourlyRequestCounts.computeIfAbsent(hourlyKey, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /**
     * Clean up old entries to prevent memory leaks
     */
    private void cleanupOldEntries() {
        LocalDateTime now = LocalDateTime.now();
        String cleanupKey = "cleanup";

        LocalDateTime lastCleanup = lastCleanupTime.get(cleanupKey);

        // Only cleanup once per hour
        if (lastCleanup != null && lastCleanup.plusHours(1).isAfter(now)) {
            return;
        }

        log.info("Cleaning up old rate limit entries...");

        // Remove entries older than 2 days
        String twoDaysAgo = LocalDate.now().minusDays(2).toString();
        dailyRequestCounts.keySet().removeIf(key -> key.contains(twoDaysAgo));

        // Remove hourly entries older than 2 hours
        int twoHoursAgo = now.minusHours(2).getHour();
        hourlyRequestCounts.keySet().removeIf(key ->
                key.contains("_" + twoHoursAgo + "_") ||
                        key.contains(twoDaysAgo)
        );

        lastCleanupTime.put(cleanupKey, now);

        log.info("Cleanup complete. Daily entries: {}, Hourly entries: {}",
                dailyRequestCounts.size(),
                hourlyRequestCounts.size());
    }
}
