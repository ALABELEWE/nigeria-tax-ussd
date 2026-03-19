package com.taxhelp.nigerian_tax_ussd.service.impl;

import com.taxhelp.nigerian_tax_ussd.model.QuestionLog;
import com.taxhelp.nigerian_tax_ussd.repository.QuestionLogRepository;
import com.taxhelp.nigerian_tax_ussd.service.QuestionLogService;
import com.taxhelp.nigerian_tax_ussd.service.ResponseEvaluationService;
import com.taxhelp.nigerian_tax_ussd.service.SecurityValidationService;
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
    private final ResponseEvaluationService responseEvaluationService;
    private final SecurityValidationService securityValidationService;

    @Override
    public void logQuestion(String sessionId, String phoneNumber, String question,
                            String answer, String language, Boolean smsDelivered,
                            Integer responseTimeMs) {
        try {
            // Evaluate response quality using your existing service
            ResponseEvaluationService.EvaluationResult quality =
                    responseEvaluationService.evaluateResponse(question, answer);

            // Check for PII in the original question
            SecurityValidationService.ValidationResult security =
                    securityValidationService.validateInput(question);

            // Build evaluation notes
            StringBuilder notes = new StringBuilder();
            if (!quality.getIssues().isEmpty()) {
                notes.append("Quality issues: ").append(String.join(", ", quality.getIssues()));
            }
            if (security.containsPii()) {
                if (notes.length() > 0) notes.append(" | ");
                notes.append("PII detected and redacted: PHONE");
            }
            if (security.hasThreats()) {
                if (notes.length() > 0) notes.append(" | ");
                notes.append("Security threats: ")
                        .append(security.getThreats().stream()
                                .map(SecurityValidationService.Threat::getType)
                                .reduce((a, b) -> a + ", " + b).orElse(""));
            }

            QuestionLog log = QuestionLog.builder()
                    .sessionId(sessionId)
                    .phoneNumber(phoneNumber)
                    .question(question)
                    .answer(answer)
                    .language(language)
                    .smsDelivered(smsDelivered)
                    .responseTimeMs(Long.valueOf(responseTimeMs))
                    .timestamp(LocalDateTime.now())
                    .qualityScore(quality.getQualityScore())
                    .isRelevant(quality.isRelevant())
                    .containsPii(security.containsPii())
                    .securityFlagged(security.hasThreats())
                    .evaluationNotes(notes.length() > 0 ? notes.toString() : null)
                    .build();

            questionLogRepository.save(log);

            this.log.info("Question logged — Phone: {}, Lang: {}, Quality: {:.2f}, PII: {}, SMS: {}",
                    phoneNumber, language, quality.getQualityScore(),
                    security.containsPii(), smsDelivered);

        } catch (Exception e) {
            log.error("Failed to log question: {}", e.getMessage());
        }
    }

    public Long getTodayQuestionCount() {
        return questionLogRepository.countTodayQuestions(LocalDate.now());
    }
}