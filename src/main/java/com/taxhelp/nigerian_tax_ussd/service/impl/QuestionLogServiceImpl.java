package com.taxhelp.nigerian_tax_ussd.service.impl;

import com.taxhelp.nigerian_tax_ussd.model.QuestionLog;
import com.taxhelp.nigerian_tax_ussd.repository.QuestionLogRepository;
import com.taxhelp.nigerian_tax_ussd.service.QuestionLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionLogServiceImpl implements QuestionLogService {

    private final QuestionLogRepository questionLogRepository;

    @Override
    public void logQuestion(String sessionId, String phoneNumber, String question, String answer, String language, Boolean smsDelivered, Integer responseTimeMs) {

        try{
            QuestionLog questionLog = new QuestionLog();
            questionLog.setSessionId(sessionId);
            questionLog.setPhoneNumber(phoneNumber);
            questionLog.setQuestion(question);
            questionLog.setAnswer(answer);
            questionLog.setLanguage(language);
            questionLog.setSmsDelivered(smsDelivered);
            questionLog.setResponseTimeMs(responseTimeMs);
            questionLog.setTimeStamp(LocalDateTime.now());

            questionLogRepository.save(questionLog);

            log.info("Question logged - Phone: {}, Language: {}, SMS: {}",
                    phoneNumber, language, smsDelivered);
        }catch (Exception e){
            log.error(" Failed to log question: {}", e.getMessage());
        }
    }

    // Get analytics data
    public Long getTodayQuestionCount() {
        return questionLogRepository.countTodayQuestions(LocalDate.now());
    }


}
