package com.taxhelp.nigerian_tax_ussd.service.impl;

import com.taxhelp.nigerian_tax_ussd.config.AfricasTalkingProperties;
import com.taxhelp.nigerian_tax_ussd.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements SmsService {

//    private final WebClient smsWebClient;
    private final AfricasTalkingProperties africasTalkingProperties;

    @Override
    public void sendSmsAsync(String phoneNumber, String message) {
        log.info("Sending sms to {} with message: {}",  phoneNumber, message);
        CompletableFuture.runAsync(() -> sendSms(phoneNumber, message));
    }

    private void sendSms(String phoneNumber, String message) {
        try{
            // Create a NEW WebClient specifically for SMS
            WebClient smsClient = WebClient.builder()
                    .baseUrl(africasTalkingProperties.getSmsUrl())
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("apiKey", africasTalkingProperties.getApiKey())
                    .build();

            // Build form data
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("username", africasTalkingProperties.getUsername());
            formData.add("to", phoneNumber);
            formData.add("message", message);
            formData.add("from", africasTalkingProperties.getSenderId());

            // DEBUG LOG
            log.info("SMS Request - Username: {}, From: {}, ApiKey: {}",
                    africasTalkingProperties.getUsername(),
                    africasTalkingProperties.getSenderId(),
                    africasTalkingProperties.getApiKey().substring(0, 10) + "...");

            log.info("Calling Africa's Talking SMS API at: {}", africasTalkingProperties.getSmsUrl());

            String response = smsClient
                    .post()
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("SMS sent successfully. Response: {}",  response);
        }catch (Exception e){
            log.error("Failed to send SMS to {}", phoneNumber, e);
        }
    }
}
