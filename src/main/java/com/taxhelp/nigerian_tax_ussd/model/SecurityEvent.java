package com.taxhelp.nigerian_tax_ussd.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for tracking security events and threats
 */
@Entity
@Table(name = "security_events", indexes = {
        @Index(name = "idx_se_phone_number", columnList = "phone_number"),
        @Index(name = "idx_se_event_type",   columnList = "event_type"),
        @Index(name = "idx_se_timestamp",    columnList = "timestamp"),
        @Index(name = "idx_se_severity",     columnList = "severity")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType; // PROMPT_INJECTION, PII_DETECTED, RATE_LIMIT_EXCEEDED, etc.

    @Column(name = "severity", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "input_sample", columnDefinition = "TEXT")
    private String inputSample; // Sanitized input sample

    @Column(name = "threat_details", columnDefinition = "TEXT")
    private String threatDetails; // JSON or detailed threat info

    @Column(name = "blocked", nullable = false)
    private boolean blocked; // Was the request blocked?

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public enum Severity {
        LOW,      // Warning, suspicious but allowed
        MEDIUM,   // Potential threat, monitored
        HIGH,     // Clear threat, blocked
        CRITICAL  // Severe attack, blocked + flagged
    }
}