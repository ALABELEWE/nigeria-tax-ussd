package com.taxhelp.nigerian_tax_ussd.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Security validation service for detecting and preventing security threats
 */
@Service
@Slf4j
public class SecurityValidationService {

    // Prompt injection patterns
    private static final Pattern[] PROMPT_INJECTION_PATTERNS = {
        Pattern.compile("(?i)ignore (previous|above|all) (instructions|prompts|commands)"),
        Pattern.compile("(?i)(you are|act as|pretend to be|roleplay as) (a|an) "),
        Pattern.compile("(?i)(system|admin|root|developer) (prompt|mode|access)"),
        Pattern.compile("(?i)disregard (your|the) (rules|guidelines|instructions)"),
        Pattern.compile("(?i)\\[SYSTEM\\]|\\[ADMIN\\]|\\[DEV\\]|\\[ROOT\\]"),
        Pattern.compile("(?i)<\\s*script|javascript:|onerror=|onclick="),
        Pattern.compile("(?i)jailbreak|bypass|override (safety|security|filters)")
    };

    // PII patterns (Nigerian context)
    private static final Pattern[] PII_PATTERNS = {
        Pattern.compile("\\b\\d{11}\\b"), // 11-digit phone numbers
        Pattern.compile("\\b\\d{10}\\b"), // 10-digit BVN
        Pattern.compile("\\b[A-Z]{2}\\d{8}\\b"), // NIN format
        Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b"), // Credit card
        Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), // Email
        Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b") // SSN-like patterns
    };

    /**
     * Validate input for security threats
     * 
     * @param input User input to validate
     * @return ValidationResult with details
     */
    public ValidationResult validateInput(String input) {
        ValidationResult result = new ValidationResult();
        
        if (input == null || input.trim().isEmpty()) {
            result.setValid(true);
            return result;
        }

        // Check for prompt injection
        for (Pattern pattern : PROMPT_INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                result.setValid(false);
                result.addThreat("PROMPT_INJECTION", "Potential prompt injection detected");
                log.warn("Prompt injection attempt detected: {}", sanitizeForLog(input));
                break;
            }
        }

        // Check for PII
        List<String> piiFound = new ArrayList<>();
        for (Pattern pattern : PII_PATTERNS) {
            if (pattern.matcher(input).find()) {
                piiFound.add(pattern.pattern());
            }
        }
        
        if (!piiFound.isEmpty()) {
            result.setContainsPii(true);
            result.addThreat("PII_DETECTED", "Personal information detected in input");
            log.info("PII detected in input (patterns: {})", piiFound.size());
        }

        // Check input length (prevent DoS)
        if (input.length() > 500) {
            result.setValid(false);
            result.addThreat("EXCESSIVE_LENGTH", "Input exceeds maximum length");
            log.warn("Excessive input length: {} characters", input.length());
        }

        // Check for suspicious characters
        if (containsSuspiciousCharacters(input)) {
            result.addWarning("SUSPICIOUS_CHARS", "Input contains unusual characters");
            log.debug("Suspicious characters detected in input");
        }

        return result;
    }

    /**
     * Sanitize PII from text for logging
     * 
     * @param text Text to sanitize
     * @return Sanitized text
     */
    public String sanitizePii(String text) {
        if (text == null) return null;
        
        String sanitized = text;
        
        // Redact phone numbers
        sanitized = sanitized.replaceAll("\\b\\d{11}\\b", "[PHONE_REDACTED]");
        sanitized = sanitized.replaceAll("\\b\\d{10}\\b", "[ID_REDACTED]");
        
        // Redact emails
        sanitized = sanitized.replaceAll(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", 
            "[EMAIL_REDACTED]"
        );
        
        // Redact credit cards
        sanitized = sanitized.replaceAll(
            "\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b", 
            "[CARD_REDACTED]"
        );
        
        return sanitized;
    }

    /**
     * Sanitize text for safe logging (prevent log injection)
     * 
     * @param text Text to sanitize
     * @return Safe text for logging
     */
    private String sanitizeForLog(String text) {
        if (text == null) return null;
        
        // Truncate long text
        String sanitized = text.length() > 100 ? text.substring(0, 100) + "..." : text;
        
        // Remove newlines and control characters
        sanitized = sanitized.replaceAll("[\\r\\n\\t]", " ");
        
        // Sanitize PII
        sanitized = sanitizePii(sanitized);
        
        return sanitized;
    }

    /**
     * Check for suspicious characters that might indicate injection attacks
     */
    private boolean containsSuspiciousCharacters(String input) {
        // Check for excessive special characters
        long specialCharCount = input.chars()
            .filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))
            .count();
        
        return specialCharCount > input.length() * 0.3; // More than 30% special chars
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private boolean valid = true;
        private boolean containsPii = false;
        private List<Threat> threats = new ArrayList<>();
        private List<Warning> warnings = new ArrayList<>();

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public boolean containsPii() {
            return containsPii;
        }

        public void setContainsPii(boolean containsPii) {
            this.containsPii = containsPii;
        }

        public List<Threat> getThreats() {
            return threats;
        }

        public List<Warning> getWarnings() {
            return warnings;
        }

        public void addThreat(String type, String message) {
            threats.add(new Threat(type, message));
        }

        public void addWarning(String type, String message) {
            warnings.add(new Warning(type, message));
        }

        public boolean hasThreats() {
            return !threats.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    public static class Threat {
        private final String type;
        private final String message;

        public Threat(String type, String message) {
            this.type = type;
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class Warning {
        private final String type;
        private final String message;

        public Warning(String type, String message) {
            this.type = type;
            this.message = message;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }
}