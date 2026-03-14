package com.taxhelp.nigerian_tax_ussd.service;

import com.taxhelp.nigerian_tax_ussd.model.SecurityEvent;
import com.taxhelp.nigerian_tax_ussd.repository.SecurityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for integrating security validation and quality evaluation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEvaluationService {

    private final SecurityValidationService securityValidationService;
    private final ResponseEvaluationService responseEvaluationService;
    private final SecurityEventRepository securityEventRepository;

    /**
     * Validate user input and log security events
     * 
     * @param input User input
     * @param phoneNumber User phone number
     * @param sessionId Session ID
     * @return Validation result
     */
    public SecurityValidationService.ValidationResult validateAndLog(
            String input, String phoneNumber, String sessionId) {
        
        SecurityValidationService.ValidationResult result = 
            securityValidationService.validateInput(input);

        // Log security events
        if (!result.isValid() || result.hasThreats()) {
            logSecurityEvent(input, phoneNumber, sessionId, result, true);
        } else if (result.hasWarnings()) {
            logSecurityEvent(input, phoneNumber, sessionId, result, false);
        }

        return result;
    }

    /**
     * Evaluate response quality
     * 
     * @param question Original question
     * @param response RAG response
     * @return Evaluation result
     */
    public ResponseEvaluationService.EvaluationResult evaluateResponse(
            String question, String response) {
        
        return responseEvaluationService.evaluateResponse(question, response);
    }

    /**
     * Log security event to database
     */
    private void logSecurityEvent(
            String input, 
            String phoneNumber, 
            String sessionId,
            SecurityValidationService.ValidationResult result,
            boolean blocked) {
        
        try {
            for (SecurityValidationService.Threat threat : result.getThreats()) {
                SecurityEvent event = SecurityEvent.builder()
                    .phoneNumber(phoneNumber)
                    .sessionId(sessionId)
                    .eventType(threat.getType())
                    .severity(getSeverity(threat.getType(), blocked))
                    .description(threat.getMessage())
                    .inputSample(securityValidationService.sanitizePii(
                        input.length() > 100 ? input.substring(0, 100) + "..." : input
                    ))
                    .threatDetails(buildThreatDetails(result))
                    .blocked(blocked)
                    .build();

                securityEventRepository.save(event);
                
                log.info("Security event logged - Type: {}, Severity: {}, Blocked: {}, Phone: {}", 
                    event.getEventType(), 
                    event.getSeverity(), 
                    blocked,
                    phoneNumber);
            }
        } catch (Exception e) {
            log.error("Failed to log security event: {}", e.getMessage(), e);
        }
    }

    /**
     * Determine severity based on threat type
     */
    private SecurityEvent.Severity getSeverity(String threatType, boolean blocked) {
        return switch (threatType) {
            case "PROMPT_INJECTION" -> SecurityEvent.Severity.CRITICAL;
            case "EXCESSIVE_LENGTH" -> SecurityEvent.Severity.HIGH;
            case "PII_DETECTED" -> SecurityEvent.Severity.MEDIUM;
            case "SUSPICIOUS_CHARS" -> SecurityEvent.Severity.LOW;
            default -> SecurityEvent.Severity.MEDIUM;
        };
    }

    /**
     * Build threat details JSON
     */
    private String buildThreatDetails(SecurityValidationService.ValidationResult result) {
        StringBuilder details = new StringBuilder("{");
        
        details.append("\"threats\": [");
        for (int i = 0; i < result.getThreats().size(); i++) {
            SecurityValidationService.Threat threat = result.getThreats().get(i);
            if (i > 0) details.append(", ");
            details.append("{")
                .append("\"type\": \"").append(threat.getType()).append("\",")
                .append("\"message\": \"").append(threat.getMessage()).append("\"")
                .append("}");
        }
        details.append("],");
        
        details.append("\"warnings\": [");
        for (int i = 0; i < result.getWarnings().size(); i++) {
            SecurityValidationService.Warning warning = result.getWarnings().get(i);
            if (i > 0) details.append(", ");
            details.append("{")
                .append("\"type\": \"").append(warning.getType()).append("\",")
                .append("\"message\": \"").append(warning.getMessage()).append("\"")
                .append("}");
        }
        details.append("],");
        
        details.append("\"containsPii\": ").append(result.containsPii());
        details.append("}");
        
        return details.toString();
    }

    /**
     * Check if user has too many security incidents
     * 
     * @param phoneNumber User phone number
     * @return true if user should be blocked
     */
    public boolean shouldBlockUser(String phoneNumber) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        
        long recentIncidents = securityEventRepository
            .countByPhoneNumberAfter(phoneNumber, oneDayAgo);
        
        if (recentIncidents >= 5) {
            log.warn("User {} has {} security incidents in last 24h - considering block", 
                phoneNumber, recentIncidents);
            return true;
        }
        
        return false;
    }

    /**
     * Get security statistics for monitoring
     */
    public SecurityStatistics getSecurityStatistics(int hours) {
        LocalDateTime after = LocalDateTime.now().minusHours(hours);
        
        var stats = securityEventRepository.getEventTypeStatistics(after);
        var highSeverityEvents = securityEventRepository.findRecentHighSeverityEvents(after);
        
        return SecurityStatistics.builder()
            .periodHours(hours)
            .totalEvents(stats.stream().mapToLong(s -> (Long) s[1]).sum())
            .highSeverityEvents(highSeverityEvents.size())
            .build();
    }

    @lombok.Data
    @lombok.Builder
    public static class SecurityStatistics {
        private int periodHours;
        private long totalEvents;
        private long highSeverityEvents;
    }
}