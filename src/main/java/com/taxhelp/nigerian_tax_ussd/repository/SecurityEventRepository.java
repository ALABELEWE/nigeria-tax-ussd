package com.taxhelp.nigerian_tax_ussd.repository;

import com.taxhelp.nigerian_tax_ussd.model.SecurityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

    /**
     * Find security events by phone number
     */
    List<SecurityEvent> findByPhoneNumber(String phoneNumber);

    /**
     * Find security events by phone number within time range
     */
    List<SecurityEvent> findByPhoneNumberAndTimestampAfter(String phoneNumber, LocalDateTime after);

    /**
     * Find security events by type
     */
    List<SecurityEvent> findByEventType(String eventType);

    /**
     * Find security events by severity
     */
    List<SecurityEvent> findBySeverity(SecurityEvent.Severity severity);

    /**
     * Find blocked security events
     */
    List<SecurityEvent> findByBlocked(boolean blocked);

    /**
     * Count security events by phone number in time range
     */
    @Query("SELECT COUNT(e) FROM SecurityEvent e WHERE e.phoneNumber = :phoneNumber AND e.timestamp > :after")
    long countByPhoneNumberAfter(@Param("phoneNumber") String phoneNumber, @Param("after") LocalDateTime after);

    /**
     * Get recent high-severity events
     */
    @Query("SELECT e FROM SecurityEvent e WHERE e.severity IN ('HIGH', 'CRITICAL') AND e.timestamp > :after ORDER BY e.timestamp DESC")
    List<SecurityEvent> findRecentHighSeverityEvents(@Param("after") LocalDateTime after);

    /**
     * Get security event statistics
     */
    @Query("SELECT e.eventType, COUNT(e) FROM SecurityEvent e WHERE e.timestamp > :after GROUP BY e.eventType")
    List<Object[]> getEventTypeStatistics(@Param("after") LocalDateTime after);
}