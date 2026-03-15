package com.taxhelp.nigerian_tax_ussd.controller;

import com.taxhelp.nigerian_tax_ussd.config.GoogleTranslationService;
import com.taxhelp.nigerian_tax_ussd.config.LanguageConfig;
import com.taxhelp.nigerian_tax_ussd.config.RagServiceProperties;
import com.taxhelp.nigerian_tax_ussd.model.Language;
import com.taxhelp.nigerian_tax_ussd.model.UserSession;
import com.taxhelp.nigerian_tax_ussd.model.request.RagQueryRequest;
import com.taxhelp.nigerian_tax_ussd.model.response.RagQueryResponse;
import com.taxhelp.nigerian_tax_ussd.service.QuestionLogService;
import com.taxhelp.nigerian_tax_ussd.service.SecurityValidationService;
import com.taxhelp.nigerian_tax_ussd.service.SessionService;
import com.taxhelp.nigerian_tax_ussd.service.SmsService;
import com.taxhelp.nigerian_tax_ussd.service.util.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ussd")
@RequiredArgsConstructor
public class UssdController {

    private final SmsService smsService;
    private final LanguageConfig languageConfig;
    private final WebClient ragWebClient;
    private final GoogleTranslationService translationService;
    private final RagServiceProperties ragProps;
    private final SessionService sessionService;
    private final RateLimiterService rateLimiterService;
    private final QuestionLogService questionLogService;
    private final SecurityValidationService securityValidationService;

    @PostMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public String handleUssdCallback(
            @RequestParam String sessionId,
            @RequestParam String phoneNumber,
            @RequestParam(required = false, defaultValue = "") String text) { // ← removed unused serviceCode

        log.info("USSD Request - SessionID: {}, PhoneNumber: {}, Text: '{}'",
                sessionId, phoneNumber, text);

        if (!text.isEmpty()) {
            String securityCheck = performSecurityCheck(text, phoneNumber);
            if (securityCheck != null) return securityCheck;

            String rateLimitCheck = performRateLimitCheck(phoneNumber);
            if (rateLimitCheck != null) return rateLimitCheck;
        }

        try {
            return processUssdRequest(sessionId, phoneNumber, text);
        } catch (Exception e) {
            log.error("USSD Error: {}", e.getMessage(), e);
            return "END Service error. Please try again later.";
        }
    }

    /**
     * Validates input for security threats.
     * Returns an END message if invalid, null if safe to proceed.
     */
    private String performSecurityCheck(String text, String phoneNumber) {
        SecurityValidationService.ValidationResult security =
                securityValidationService.validateInput(text);

        if (security.isValid()) return null;

        log.warn("Security validation failed for PhoneNumber: {} - Threats: {}",
                phoneNumber, security.getThreats().size());

        boolean isInjection = security.getThreats().stream()
                .anyMatch(t -> "PROMPT_INJECTION".equals(t.getType()));
        boolean isPii = security.getThreats().stream()
                .anyMatch(t -> "PII_DETECTED".equals(t.getType()));
        boolean isTooLong = security.getThreats().stream()
                .anyMatch(t -> "EXCESSIVE_LENGTH".equals(t.getType()));

        if (isInjection) {
            return "END Invalid input detected. Please enter a valid tax question.";
        }
        if (isPii) {
            return "END Please do not include personal information such as phone numbers or emails in your question.";
        }
        if (isTooLong) {
            return "END Your question is too long. Please keep it under 500 characters.";
        }
        return "END Invalid input. Please dial again with a valid tax question.";
    }

    /**
     * Checks rate limit for the phone number.
     * Returns an END message if exceeded, null if safe to proceed.
     */
    private String performRateLimitCheck(String phoneNumber) {
        if (rateLimiterService.allowRequest(phoneNumber)) return null;

        int remaining = rateLimiterService.getRemainingRequests(phoneNumber);
        if (remaining == 0) {
            return "END You have reached your daily limit of 20 questions. Please try again tomorrow.";
        }
        return "END You are sending questions too quickly. Please wait a few minutes and try again.";
    }

    /**
     * Routes the USSD request to the appropriate handler based on session state.
     */
    private String processUssdRequest(String sessionId, String phoneNumber, String text) {
        UserSession session = sessionService.getOrCreate(sessionId, phoneNumber);

        String response;
        if (text.isEmpty()) {
            response = buildLanguageMenu();
        } else if (session.getLanguage() == null) {
            response = handleLanguageSelection(session, text);
        } else {
            response = handleQuestionSubmission(session, text);
        }

        log.info("USSD Response: {}", response.substring(0, Math.min(60, response.length())));
        return response;
    }

    private String handleQuestionSubmission(UserSession session, String text) {
        String question = extractQuestion(text);

        if (question.trim().isEmpty()) {
            return "END Invalid question. Please dial again.";
        }

        String userLanguage = session.getLanguage();
        String sanitizedQuestion = securityValidationService.sanitizePii(question);

        log.info("Question Received - SessionID: {}, Language: {}, Question: {}",
                session.getSessionId(), userLanguage, sanitizedQuestion);

        processQuestionAsync(session.getSessionId(), session.getPhoneNumber(), question, userLanguage);
        sessionService.delete(session.getSessionId());

        return "END " + getConfirmationMessage(userLanguage); // ← removed unused question param
    }

    private String handleLanguageSelection(UserSession session, String languageOption) {
        Language language = languageConfig.getLanguageByOption(languageOption);

        if (language == null) {
            return "END Invalid selection. Please dial again";
        }

        sessionService.setLanguage(session.getSessionId(), language.getCode());

        // Fixed: was passing 3 args for 2 placeholders
        log.info("Language Selection - SessionID: {}, Language: {}",
                session.getSessionId(), language.getName());

        return "CON " + translatePrompt(language.getCode());
    }

    private String translatePrompt(String languageCode) {
        Map<String, String> prompts = Map.of(
                "en", "Enter your tax question:\n\nExample: What is VAT rate?",
                "yo", "Tẹ ìbéèrè owó-orí rẹ sílẹ̀:\n\nÀpẹẹrẹ: Kí ni oṣùwọ̀n VAT?",
                "ig", "Tinye ajụjụ ụtụ isi gị:\n\nỌmụmaatụ: Gịnị bụ ọnụego VAT?",
                "ha", "Shigar da tambayar harajin ku:\n\nMisali: Menene ƙimar VAT?"
        );
        return prompts.getOrDefault(languageCode, prompts.get("en"));
    }

    private String buildLanguageMenu() {
        StringBuilder menu = new StringBuilder("CON Welcome to Nigeria Tax Help \n");
        menu.append("Select your language:\n\n");
        languageConfig.getAllLanguages().forEach((option, language) ->
                menu.append(option).append(". ").append(language.getName()).append("\n"));
        return menu.toString().trim();
    }

    // ← removed unused `question` parameter
    private String getConfirmationMessage(String languageCode) {
        Map<String, String> messages = Map.of(
                "en", "Thank you! Processing your question. Answer will be sent via SMS shortly.",
                "yo", "Ẹ ṣeun! Ń ṣiṣẹ́ lórí ìbéèrè rẹ. A ó fi ìdáhùn ránṣẹ́ nípasẹ̀ SMS láìpẹ́.",
                "ig", "Daalụ! Na-edozi ajụjụ gị. A ga-eziga azịza site na SMS n'oge na-adịghị anya.",
                "ha", "Na gode! Ana aiki akan tambayar ku. Za a aika amsa ta SMS nan ba da jimawa ba."
        );
        return messages.getOrDefault(languageCode, messages.get("en"));
    }

    private void processQuestionAsync(String sessionId, String phoneNumber,
                                      String question, String userLanguage) {
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            boolean smsDelivered = false;
            String finalAnswer = null;

            try {
                log.info("Processing Question Async - SessionID: {}, PhoneNumber: {}",
                        sessionId, phoneNumber);

                String questionInEnglish = translateToEnglishIfNeeded(question, userLanguage);

                RagQueryResponse response = queryRagService(questionInEnglish);

                if (response == null || !response.getSuccess()) {
                    finalAnswer = "Error: RAG service failed";
                    sendErrorSms(phoneNumber, userLanguage);
                    return;
                }

                String answerInUserLanguage = translateFromEnglishIfNeeded(
                        response.getAnswer(), userLanguage);
                finalAnswer = answerInUserLanguage;

                String sanitizedAnswer = securityValidationService.sanitizePii(answerInUserLanguage);
                smsService.sendSmsAsync(phoneNumber, formatSmsMessage(sanitizedAnswer, userLanguage));

                smsDelivered = true;
                log.info("Complete! SMS sent to: {}", phoneNumber);

            } catch (Exception e) {
                log.error("Error processing Question Async - SessionID: {}, PhoneNumber: {}",
                        sessionId, phoneNumber, e);
                finalAnswer = "Error: " + e.getMessage();
                sendErrorSms(phoneNumber, userLanguage);
            } finally {
                logQuestion(sessionId, phoneNumber, question, finalAnswer, userLanguage,
                        smsDelivered, startTime);
            }
        }, "ussd-processor-" + sessionId).start();
    }

    private String translateToEnglishIfNeeded(String text, String languageCode) {
        if ("en".equals(languageCode)) return text;
        log.info("Translating question from {} to en", languageCode);
        return translationService.translate(text, languageCode, "en");
    }

    private String translateFromEnglishIfNeeded(String text, String languageCode) {
        if ("en".equals(languageCode)) return text;
        log.info("Translating answer from en to {}", languageCode);
        return translationService.translate(text, "en", languageCode);
    }

    private RagQueryResponse queryRagService(String questionInEnglish) {
        RagQueryRequest request = RagQueryRequest.builder()
                .question(questionInEnglish)
                .maxLength(ragProps.getMaxLength())
                .build();

        return ragWebClient
                .post()
                .uri(ragProps.getQueryEndpoint())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RagQueryResponse.class)
                .block();
    }

    private void logQuestion(String sessionId, String phoneNumber, String question,
                             String finalAnswer, String userLanguage,
                             boolean smsDelivered, long startTime) {
        int responseTime = (int) (System.currentTimeMillis() - startTime);
        questionLogService.logQuestion(
                sessionId,
                phoneNumber,
                securityValidationService.sanitizePii(question),
                finalAnswer != null ? finalAnswer : "No answer generated",
                userLanguage,
                smsDelivered,
                responseTime
        );
        log.info("Question logged - ResponseTime: {}ms, SMS: {}", responseTime, smsDelivered);
    }

    private String formatSmsMessage(String answer, String languageCode) {
        String prefix = "en".equals(languageCode)
                ? "Tax Help:\n\n"
                : translationService.translate("Tax Help:", "en", languageCode) + "\n\n";
        return prefix + answer;
    }

    private void sendErrorSms(String phoneNumber, String languageCode) {
        String errorMessage = "Sorry, we couldn't process your question. Please try again.";
        if (!"en".equals(languageCode)) {
            errorMessage = translationService.translate(errorMessage, "en", languageCode);
        }
        smsService.sendSmsAsync(phoneNumber, errorMessage);
    }

    private String extractQuestion(String fullText) {
        if (fullText == null || fullText.isEmpty()) return "";
        String[] parts = fullText.split("\\*");
        String question = parts[parts.length - 1].trim();
        log.info("Extracted question: '{}' from full text: '{}'", question, fullText);
        return question;
    }
}