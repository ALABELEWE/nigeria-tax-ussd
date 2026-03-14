package com.taxhelp.nigerian_tax_ussd.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Enhanced QuestionLog entity with quality metrics
 * Add these new columns to your existing QuestionLog entity
 */
@Entity
@Table(name = "question_logs", indexes = {
        @Index(name = "idx_phone_number", columnList = "phone_number"),
        @Index(name = "idx_timestamp", columnList = "timestamp"),
        @Index(name = "idx_language", columnList = "language"),
        @Index(name = "idx_quality_score", columnList = "quality_score")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "language", nullable = false, length = 5)
    private String language;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "response_time_ms", nullable = false)
    private Long responseTimeMs;

    @Column(name = "sms_delivered", nullable = false)
    private boolean smsDelivered;

    // ==================== NEW QUALITY METRICS ====================

    @Column(name = "quality_score")
    private Double qualityScore; // 0.0 to 1.0

    @Column(name = "is_relevant")
    private Boolean isRelevant;

    @Column(name = "contains_pii")
    private Boolean containsPii;

    @Column(name = "security_flagged")
    private Boolean securityFlagged;

    @Column(name = "evaluation_notes", columnDefinition = "TEXT")
    private String evaluationNotes; // Quality issues or warnings

    @Column(name = "answer_length")
    private Integer answerLength;

    @Column(name = "translation_time_ms")
    private Long translationTimeMs;

    @Column(name = "rag_query_time_ms")
    private Long ragQueryTimeMs;

    @Column(name = "unicode_sms")
    private Boolean unicodeSms; // Was Unicode encoding used?

    @Column(name = "sms_segments")
    private Integer smsSegments; // Number of SMS segments

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (answer != null && answerLength == null) {
            answerLength = answer.length();
        }
    }
}