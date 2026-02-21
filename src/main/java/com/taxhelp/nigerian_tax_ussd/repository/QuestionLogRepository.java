package com.taxhelp.nigerian_tax_ussd.repository;

import com.taxhelp.nigerian_tax_ussd.model.QuestionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuestionLogRepository extends JpaRepository<QuestionLog, Long> {

    // Finds logs by phone number
    List<QuestionLog> findByPhoneNumberOrderByTimestampDesc(String phoneNumber);

    // Find logs within date range
    List<QuestionLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Count questions today
    @Query("SELECT COUNT (q) FROM QuestionLog q WHERE DATE(q.timeStamp) = CURRENT_DATE ")
    Long countTodayQuestions();

    // Count by langauge
    @Query("SELECT q.language, COUNT(q) FROM QuestionLog q GROUP BY q.language")
    List<Object[]> countByLanguage();

    @Query("SELECT q.question, COUNT(q) as cnt FROM QuestionLog q GROUP BY q.question ORDER BY cnt DESC ")
    List<Object[]> findMostAskedQuestions();


}
