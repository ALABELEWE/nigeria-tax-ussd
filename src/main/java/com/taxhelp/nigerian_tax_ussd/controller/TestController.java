package com.taxhelp.nigerian_tax_ussd.controller;


import com.taxhelp.nigerian_tax_ussd.config.RagServiceProperties;
import com.taxhelp.nigerian_tax_ussd.model.request.RagQueryRequest;
import com.taxhelp.nigerian_tax_ussd.model.response.RagQueryResponse;
import com.taxhelp.nigerian_tax_ussd.service.RagClientService;
import com.taxhelp.nigerian_tax_ussd.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final RagClientService ragClientService;
    private final SmsService smsService;
    private final WebClient ragWebClient;
    private final RagServiceProperties ragServiceProperties;

    @GetMapping("/rag")
    public RagQueryResponse testRag(@RequestParam String question) {
        log.info("Testing RAG with question: {}", question);
        return ragClientService.queryTaxAssistant(question);
    }

    @PostMapping("/ussd-simulate")
    public Map<String, String> simulateUssd(
            @RequestParam String phoneNumber,
            @RequestParam String question
    ) {
        log.info("Simulating USSD - Phone: {}, Question: {}", phoneNumber, question);


        // Process in background thread - NO BLOCKING
        new Thread(() -> {
            try {
                log.info("Simulating USSD - Phone: {}, Question: {}", phoneNumber, question);

                RagQueryRequest request = RagQueryRequest.builder()
                        .question(question)
                        .maxLength(ragServiceProperties.getMaxLength())
                        .build();

                // Query RAG
                RagQueryResponse response = ragWebClient
                        .post()
                        .uri(ragServiceProperties.getQueryEndpoint())
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(RagQueryResponse.class)
                        .timeout(Duration.ofMinutes(5))
                        .block();

                log.info("Got response: {}", response.getSuccess());

                String smsMessage = "Tax Help Answer: \n\n" + response.getAnswer();
                smsService.sendSmsAsync(phoneNumber, smsMessage);
            }catch (Exception ex){
                log.error("Exception occurred while sending sms message", ex);
                smsService.sendSmsAsync(phoneNumber, "SMS Error");
            }
        }).start();



        // Send immediately
        Map<String, String> result = new HashMap<>();
        result.put("status", "success");
        result.put("phone", phoneNumber);
        result.put("question", question);
        result.put("message", "Question submitted for processing. Check logs for progress.");

        return result;
    }
}