package com.taxhelp.nigerian_tax_ussd.repository;

import com.taxhelp.nigerian_tax_ussd.model.QuestionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionLogRepository extends JpaRepository<QuestionLog, Long> {

    // ── Existing methods ──────────────────────────────────────────────────────

    @Query("SELECT COUNT(q) FROM QuestionLog q WHERE CAST(q.timestamp AS LocalDate) = :today")
    Long countTodayQuestions(@Param("today") LocalDate today);

    @Query("SELECT q.language, COUNT(q) FROM QuestionLog q GROUP BY q.language")
    List<Object[]> countByLanguage();

    // ── Quality queries ───────────────────────────────────────────────────────

    @Query("SELECT AVG(q.qualityScore) FROM QuestionLog q WHERE q.qualityScore IS NOT NULL")
    Double findAverageQualityScore();

    @Query(value = "SELECT PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY quality_score) FROM question_logs WHERE quality_score IS NOT NULL",
            nativeQuery = true)
    Double findMedianQualityScore();

    long countByQualityScoreIsNotNull();

    long countByIsRelevantTrue();

    long countByQualityScoreLessThan(double threshold);

    @Query("SELECT COUNT(q) FROM QuestionLog q WHERE q.qualityScore >= :min AND q.qualityScore < :max")
    long countByQualityScoreBetween(@Param("min") double min, @Param("max") double max);

    Page<QuestionLog> findByQualityScoreLessThanOrderByQualityScoreAsc(
            double threshold, Pageable pageable);

    // ── Analytics queries ─────────────────────────────────────────────────────

    @Query("SELECT AVG(q.responseTimeMs) FROM QuestionLog q WHERE q.responseTimeMs IS NOT NULL")
    Double findAverageResponseTimeMs();

    @Query(value = """
        SELECT
            DATE(timestamp)             AS day,
            COUNT(DISTINCT session_id)  AS sessions,
            COUNT(*)                    AS questions,
            AVG(quality_score)          AS avg_quality,
            AVG(response_time_ms)       AS avg_response
        FROM question_logs
        WHERE timestamp BETWEEN :from AND :to
        GROUP BY DATE(timestamp)
        ORDER BY DATE(timestamp) ASC
    """, nativeQuery = true)
    List<Object[]> findDailyStats(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query(value = """
        SELECT
            EXTRACT(HOUR FROM timestamp)    AS hour,
            COUNT(DISTINCT session_id)      AS sessions
        FROM question_logs
        WHERE timestamp >= :startOfDay
        GROUP BY EXTRACT(HOUR FROM timestamp)
        ORDER BY hour ASC
    """, nativeQuery = true)
    List<Object[]> findHourlyStats(@Param("startOfDay") LocalDateTime startOfDay);

    // ── PII queries ───────────────────────────────────────────────────────────

    long countByContainsPiiTrue();

    long countByContainsPiiTrueAndTimestampAfter(LocalDateTime timestamp);

    Page<QuestionLog> findByContainsPiiTrueOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT COUNT(q) FROM QuestionLog q WHERE q.containsPii = true AND q.evaluationNotes LIKE %:piiType%")
    long countPiiByType(@Param("piiType") String piiType);

    @Query(value = """
        SELECT
            DATE(timestamp) AS day,
            COUNT(*)        AS cnt
        FROM question_logs
        WHERE contains_pii = true
          AND timestamp >= :from
        GROUP BY DATE(timestamp)
        ORDER BY DATE(timestamp) ASC
    """, nativeQuery = true)
    List<Object[]> findDailyPiiCounts(@Param("from") LocalDateTime from);
}