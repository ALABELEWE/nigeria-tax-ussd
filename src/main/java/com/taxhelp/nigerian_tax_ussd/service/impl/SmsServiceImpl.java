package com.taxhelp.nigerian_tax_ussd.service.impl;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import com.taxhelp.nigerian_tax_ussd.config.AfricasTalkingProperties;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsServiceImpl implements com.taxhelp.nigerian_tax_ussd.service.SmsService {

    private final AfricasTalkingProperties africasTalkingProperties;
    private SmsService smsService;

    @PostConstruct
    public void init() {
        // Initialize the SDK once at startup
        AfricasTalking.initialize(
                africasTalkingProperties.getUsername(),
                africasTalkingProperties.getApiKey()
        );

        // Get the SMS service
        smsService = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);

        log.info("Africa's Talking ADK initialized with username: {}",
                africasTalkingProperties.getUsername());

    }

    @Override
    public void sendSmsAsync(String phoneNumber, String message) {
        log.info("Sending SMS to {} with message: {}", phoneNumber, message);
        CompletableFuture.runAsync(() -> sendSms(phoneNumber, message));
    }

    private void sendSms(String phoneNumber, String message) {
        try {
            // Set recipients (SDK requires array)
            String[] recipients = new String[]{phoneNumber};

            // Set sender ID (can be null for default)
            String from = africasTalkingProperties.getSenderId();

            log.info("Calling Africa's Talking SMS API - From: {}, To: {}", from, phoneNumber);

            // Send SMS using SDK
            List<Recipient> response = smsService.send(message, from, recipients, true);

            for (Recipient recipient : response) {
                log.info("SMS Result - Number: {}. Status: {}, MessageId: {}, Cost: {}",
                        recipient.number,
                        recipient.status,
                        recipient.messageId,
                        recipient.cost);

                if ("Success".equalsIgnoreCase(recipient.status)) {
                    log.info("SMS sent successfully to {}", recipient.number);
                }else {
                    log.error("SMS failed to {}: {}", recipient.number, recipient.status);
                }
            }
        }catch (Exception ex){
            log.error("Failed to send SMS to {}: {}", phoneNumber, ex.getMessage(), ex);
        }
    }


}
