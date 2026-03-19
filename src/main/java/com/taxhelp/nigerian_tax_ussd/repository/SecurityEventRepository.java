package com.taxhelp.nigerian_tax_ussd.repository;

import com.taxhelp.nigerian_tax_ussd.model.SecurityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    // Existing methods — kept as-is
    List<SecurityEvent> findByPhoneNumber(String phoneNumber);
    List<SecurityEvent> findByPhoneNumberAndTimestampAfter(String phoneNumber, LocalDateTime after);
    List<SecurityEvent> findByEventType(String eventType);
    List<SecurityEvent> findBySeverity(SecurityEvent.Severity severity);
    List<SecurityEvent> findByBlocked(boolean blocked);

    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.phoneNumber = :phoneNumber AND e.timestamp > :after")
    long countByPhoneNumberAfter(@Param("phoneNumber") String phoneNumber, @Param("after") LocalDateTime after);

    @Query("SELECT e FROM SecurityEvent e WHERE e.severity IN ('HIGH', 'CRITICAL') AND e.timestamp > :after ORDER BY e.timestamp DESC")
    List<SecurityEvent> findRecentHighSeverityEvents(@Param("after") LocalDateTime after);

    @Query("SELECT e.eventType, COUNT(e) FROM SecurityEvent e WHERE e.timestamp > :after GROUP BY e.eventType")
    List<Object[]> getEventTypeStatistics(@Param("after") LocalDateTime after);

    // ── New methods needed by SecurityMetricsService ──────────────────────────

    long countByTimestampAfter(LocalDateTime timestamp);

    List<SecurityEvent> findTopNByOrderByTimestampDesc(Pageable pageable);

    @Query("""
        SELECT e FROM SecurityEvent e
        WHERE (:eventType IS NULL OR e.eventType = :eventType)
          AND (:severity  IS NULL OR CAST(e.severity AS string) = :severity)
          AND (:from      IS NULL OR e.timestamp >= :from)
          AND (:to        IS NULL OR e.timestamp <= :to)
        ORDER BY e.timestamp DESC
    """)
    Page<SecurityEvent> findByFilters(
            @Param("eventType") String eventType,
            @Param("severity")  String severity,
            @Param("from")      LocalDateTime from,
            @Param("to")        LocalDateTime to,
            Pageable pageable
    );

    @Query("SELECT e.eventType, COUNT(e) FROM SecurityEvent e GROUP BY e.eventType")
    List<Object[]> countGroupByEventTypeRaw();

    @Query("SELECT e.severity, COUNT(e) FROM SecurityEvent e GROUP BY e.severity")
    List<Object[]> countGroupBySeverityRaw();

    @Query("""
        SELECT e.phoneNumber, COUNT(e), MAX(e.timestamp)
        FROM SecurityEvent e
        GROUP BY e.phoneNumber
        ORDER BY COUNT(e) DESC
    """)
    List<Object[]> findTopOffendersRaw(Pageable pageable);
}