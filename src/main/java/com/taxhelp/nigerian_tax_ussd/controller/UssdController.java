package com.taxhelp.nigerian_tax_ussd.controller;


import com.taxhelp.nigerian_tax_ussd.config.GoogleTranslationService;
import com.taxhelp.nigerian_tax_ussd.config.LanguageConfig;
import com.taxhelp.nigerian_tax_ussd.config.RagServiceProperties;
import com.taxhelp.nigerian_tax_ussd.model.Language;
import com.taxhelp.nigerian_tax_ussd.model.UserSession;
import com.taxhelp.nigerian_tax_ussd.model.request.RagQueryRequest;
import com.taxhelp.nigerian_tax_ussd.model.response.RagQueryResponse;
import com.taxhelp.nigerian_tax_ussd.service.SessionService;
import com.taxhelp.nigerian_tax_ussd.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

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

    @PostMapping(value = "/callback", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleUssdCallback(
            @RequestParam String sessionId,
            @RequestParam String serviceCode,
            @RequestParam String phoneNumber,
            @RequestParam(required = false, defaultValue = "") String text

    ) {
        log.info("USSD Request - SessionID: {}, PhoneNumber: {}, Text: '{}'",
                sessionId, phoneNumber, text);

        try{
            // Get or create session in Redis
            UserSession session = sessionService.getOrCreate(sessionId, phoneNumber);
            String response;

            if (text.isEmpty()){
                response = buildLanguageMenu();
            }else if (session.getLanguage() == null){
                response =  handleLanguageSelection(session, text);
            }else {
                response = handleQuestionSubmission(session, text);
            }

            log.info("USSD Response: {}", response.substring(0, Math.min(60, response.length())));
            return ResponseEntity.ok(response);
        }catch (Exception e){
            log.error("USSD Error: {}", e.getMessage(), e);
            return ResponseEntity.ok("END Service error. Please try again later.");
        }

    }

    private String handleQuestionSubmission(UserSession session, String text) {
        String question = extractQuestion(text);

        if (question.trim().isEmpty()){
            return "END Invalid question. Please dial again.";
        }

        String userLanguage = session.getLanguage();

        log.info(" Question Received - SessionID: {}, Language: {}, Question: {}", session.getSessionId(), userLanguage, question );

        // Process async
        processQuestionAsync(session.getSessionId(), session.getPhoneNumber(), question, userLanguage);

        // Delete session
        sessionService.delete(session.getSessionId());

        String confirmationMessage = getConfirmationMessage(userLanguage, question);

        return "END " + confirmationMessage;
    }



    private String handleLanguageSelection(UserSession session, String languageOption) {

        Language language = languageConfig.getLanguageByOption(languageOption);

        if (language == null) {
            return "END Invalid selection. Please dial again";
        }

        // Store selected language to Redis session
        sessionService.setLanguage(session.getSessionId(), language.getCode());
        log.info("Language Selection - SessionID: {}, Language: ({})", session.getSessionId(), language.getName(),language.getCode());

        String prompt = translatePrompt(language.getCode());

        return "CON " + prompt;
    }

    private String translatePrompt(String languageCode) {
        String englishPrompt = "Enter your tax question: \n\nExample: What is VAT rate?";

        if (languageCode.equals("en")){
            return englishPrompt;
        }
        return translationService.translate(englishPrompt, "en",  languageCode);
    }

    private String buildLanguageMenu() {
        StringBuilder menu = new StringBuilder("CON Welcome to Nigeria Tax Help \n");
        menu.append("Select your language:\n\n");
        languageConfig.getAllLanguages().forEach((option, language) -> {
            menu.append(option).append(". ").append(language.getName()).append("\n");
        });
        return menu.toString().trim();
    }


    private String getConfirmationMessage(String languageCode, String question) {
        String englishMessage = "Thank you! Processing your question: \n\n\"" +
                truncate(question, 40) + "\"\n\nAnswer will be sent via SMS. ";
        if (languageCode.equals("en")) {
            return englishMessage;
        }
        return translationService.translate(englishMessage, "en", languageCode);
    }

    private void processQuestionAsync(String sessionId, String phoneNumber,
                                      String question, String userLanguage) {
        new Thread(() -> {
            try{
                log.info("Processing Question Async - SessionID: {}, PhoneNumber: {}", sessionId, phoneNumber);

                // Translate question to English (if needed)
                String questionInEnglish = question;
                if (!userLanguage.equals("en")) {
                    log.info("Translating question from {} to en", userLanguage);
                    questionInEnglish = translationService.translate(questionInEnglish, userLanguage, "en");
                    log.info("Question In English Async - PhoneNumber: {}", phoneNumber);
                }

                log.info(" Querying RAG system...");
                RagQueryRequest request = RagQueryRequest.builder()
                        .question(questionInEnglish)
                        .maxLength(ragProps.getMaxLength())
                        .build();

                RagQueryResponse response = ragWebClient
                        .post()
                        .uri(ragProps.getQueryEndpoint())
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(RagQueryResponse.class)
                        .block();

                if (response == null || !response.getSuccess()) {
                    sendErrorSms(phoneNumber, userLanguage);
                    return;
                }

                String answerInEnglish = response.getAnswer();
                log.info("RAG Response - Answer: '{}'",
                        answerInEnglish.substring(0, Math.min(100, answerInEnglish.length())));


                String answerInUserLanguage = answerInEnglish;
                if (!userLanguage.equals("en")) {
                    log.info("Translating question from en to {}", userLanguage);
                    answerInUserLanguage = translationService.translate(answerInEnglish, "en", userLanguage);
                    log.info(" Answer In {}: '{}'", userLanguage, answerInUserLanguage);
                }

                String smsMessage = formatSmsMessage(answerInUserLanguage, userLanguage);
                smsService.sendSmsAsync(phoneNumber, smsMessage);

                log.info("Complete! SMS sent to: {}", phoneNumber);
            }catch (Exception e){
                log.error("Error processing Question Async - SessionID: {}, PhoneNumber: {}", sessionId, phoneNumber);
                sendErrorSms(phoneNumber, userLanguage);
            }
        }, "ussd-processor " + sessionId).start();
    }

    private String formatSmsMessage(String answer, String languageCode) {
        String prefix = "Tax Help:\n\n";
        if (!languageCode.equals("en")) {
            prefix = translationService.translate("Tax Help:", "en", languageCode) + "\n\n";
        }
        return prefix + answer;
    }

    private void sendErrorSms(String phoneNumber, String languageCode) {
        String errorMessage = "Sorry, we couldn't process your question. Please try again.";
        if (!languageCode.equals("en")) {
            errorMessage = translationService.translate(errorMessage, "en", languageCode);
        }
        smsService.sendSmsAsync(phoneNumber, errorMessage);
    }

    private String truncate(String text, int maxLength) {
        if(text.length() <= maxLength){
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String extractQuestion(String fullText){
        if(fullText == null || fullText.isEmpty()){
            return "";
        }

        String[] parts = fullText.split("\\*");
        String question = parts[parts.length - 1].trim();

        log.info("Extracted question: '{}' from full text: '{}'", question, fullText);
        return question;
    }


}
