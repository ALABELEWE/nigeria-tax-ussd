package com.taxhelp.nigerian_tax_ussd.service;


import com.taxhelp.nigerian_tax_ussd.model.UserSession;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${session.timeout:300}")
    private long sessionTimeoutSeconds;

    private static final String SESSION_KEY_PREFIX = "session:";

    // Get or create a new session
    public UserSession getOrCreate(String sessionId, String phoneNumber) {
        String key = SESSION_KEY_PREFIX + sessionId;

        UserSession userSession = (UserSession) redisTemplate.opsForValue().get(key);

        if (userSession == null) {
            log.info("Creating new session for sessionId={}, phoneNumber={}", sessionId, phoneNumber);
            userSession = UserSession.builder()
                    .sessionId(sessionId)
                    .phoneNumber(phoneNumber)
                    .stage(UserSession.SessionStage.INITIAL)
                    .createdAt(Instant.now())
                    .lastAccessedAt(Instant.now())
                    .build();
        }else {
            log.info("Session already exist for sessionId={}, phoneNumber={}", sessionId, phoneNumber);
            userSession.setLastAccessedAt(Instant.now());
        }

        save(userSession);

        return userSession;
    }

    // Get session by ID
    public Optional<UserSession> get(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        UserSession userSession = (UserSession) redisTemplate.opsForValue().get(key);

        if (userSession != null) {
            userSession.setLastAccessedAt(Instant.now());
            save(userSession);
        }

        return Optional.ofNullable(userSession);
    }

    public void save(UserSession userSession) {
        String key = SESSION_KEY_PREFIX + userSession.getSessionId();
        redisTemplate.opsForValue().set(
                key,
                userSession,
                sessionTimeoutSeconds,
                TimeUnit.SECONDS);

        log.info("Session has been saved with sessionId={} (TTL: {}s)", userSession.getSessionId(), sessionTimeoutSeconds);
    }

    // Set language for session
    public void setLanguage(String sessionId, String languageCode) {
        get(sessionId).ifPresent(session -> {
            session.setLanguage(languageCode);
            session.setStage(UserSession.SessionStage.LANGUAGE_SELECTED);
            save(session);
            log.info("Session {}: Language set to {}", sessionId, languageCode);
        });
    }

    // Get langauge from session
    public String getLanguage(String sessionId) {
        return get(sessionId)
                .map(UserSession::getLanguage)
                .orElse("en"); // Default to English
    }

    // Set phone number for session
    public void setPhoneNumber(String sessionId, String phoneNumber) {
        get(sessionId).ifPresent(session -> {
            session.setPhoneNumber(phoneNumber);
            save(session);
            log.info("Session {}: Phone number set", sessionId);
        });
    }

    // Get phone number from session
    public String getPhoneNumber(String sessionId) {
        return get(sessionId)
                .map(UserSession::getPhoneNumber)
                .orElse(null);
    }

    // Update session stage
    public void setStage(String sessionId, UserSession.SessionStage stage) {
        get(sessionId).ifPresent(session -> {
            session.setStage(stage);
            save(session);
            log.info("Session {}: Stage set to {}", sessionId, stage);
        });
    }

    // Delete session
    public void delete(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        Boolean deleted = redisTemplate.delete(key);

        if (Boolean.TRUE.equals(deleted)) {
            log.info("Session has been deleted with sessionId={}", sessionId);
        }
    }

    // Clear all sessions (use with caution)
    public void clearAll(){

        try {
            Long deletedCount = redisTemplate.execute((RedisCallback<Long>) connection ->{
                long count = 0;
                ScanOptions options = ScanOptions.scanOptions()

                        .match(SESSION_KEY_PREFIX + "*")
                        .count(100)
                        .build();
                Cursor<byte[]> cursor = connection.scan(options);
                while (cursor.hasNext()) {
                    connection.del(cursor.next());
                    count++;
                }
                cursor.close();
                return count;
            });
            log.warn("Session has been cleared with {} records", deletedCount);
        }catch (Exception e){
            log.error("Failed to clear sessions: {}", e.getMessage());
        }
    }
    // Get count of active sessions
    public long getActiveSessionCount(){

        try {
            Long count = redisTemplate.execute((RedisCallback<Long>) connection ->{
                long sessionCount = 0;
                ScanOptions options = ScanOptions.scanOptions()
                        .match(SESSION_KEY_PREFIX + "*")
                        .count(100)
                        .build();
                Cursor<byte[]> cursor = connection.scan(options);
                while (cursor.hasNext()) {
                    cursor.next();
                    sessionCount++;
                }
                cursor.close();
                return sessionCount;
            });
            return count != null ? count : 0;
        }catch (Exception e){
            log.warn("Failed to get active session count: {}", e.getMessage());
            return -1;
        }
    }

    // Check if sessoin exists
    public boolean exists(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    // Get remaining TTL for a session in seconds
    public long getTTL(String sessionId) {
        String key = SESSION_KEY_PREFIX + sessionId;
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null ? ttl : -1;
    }

    // Extend session TTL
    public void extendTTL(String sessionId){
        get(sessionId).ifPresent(session -> {
            save(session); // This resets the TTL
            log.debug("Session {}: TTL extend to {}s", sessionId, sessionTimeoutSeconds);
        });
    }

    // Scheduled cleanup of expired sessions (backup to Redis TTL)
    @Scheduled(fixedRateString = "${session.cleanup-interval:60}000")
    public void cleanupExpiredSessions(){
        long count = getActiveSessionCount();
        log.debug("Active sessions: {}", count);
    }

    // Get session statistics
    public SessionStats getStats(){
        long activeSessionCount = getActiveSessionCount();

        return SessionStats.builder()
                .activeSessions(activeSessionCount)
                .sessionTimeoutSeconds(sessionTimeoutSeconds)
                .timestamp(Instant.now())
                .build();
    }


    // Inner class for session statistics
    @Data
    @Builder
    public static class SessionStats {
        private long activeSessions;
        private long sessionTimeoutSeconds;
        private Instant timestamp;
    }


}
