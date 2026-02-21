package com.taxhelp.nigerian_tax_ussd.service;


public interface QuestionLogService {
    void logQuestion(String sessionId, String phoneNumber, String question,
                     String answer, String language, Boolean smsDelivered,
                     Integer responseTimeMs);


}
