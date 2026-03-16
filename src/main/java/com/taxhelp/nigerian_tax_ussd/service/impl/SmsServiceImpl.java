package com.taxhelp.nigerian_tax_ussd.service.impl;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import com.africastalking.sms.Recipient;
import com.taxhelp.nigerian_tax_ussd.config.AfricasTalkingProperties;
import com.taxhelp.nigerian_tax_ussd.util.YorubaTextUtil;
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
        AfricasTalking.initialize(
                africasTalkingProperties.getUsername(),
                africasTalkingProperties.getApiKey()
        );
        smsService = AfricasTalking.getService(AfricasTalking.SERVICE_SMS);
        log.info("Africa's Talking ADK initialized with username: {}",
                africasTalkingProperties.getUsername());
    }

    @Override
    public void sendSmsAsync(String phoneNumber, String message) {
        log.info("Queueing SMS to {}", phoneNumber);
        CompletableFuture.runAsync(() -> sendSms(phoneNumber, message));
    }

    private void sendSms(String phoneNumber, String message) {
        try {
            boolean hadDiacritics = YorubaTextUtil.containsYorubaDiacritics(message);

            // Transliterate Yoruba diacritics for MTN Nigeria compatibility
            // MTN strips Unicode characters, so we remove diacritics for clean delivery
            String smsMessage = hadDiacritics
                    ? YorubaTextUtil.removeDiacritics(message)
                    : message;

            if (hadDiacritics) {
                log.info("Yoruba diacritics transliterated for MTN compatibility");
                log.debug("Original: {} | Transliterated: {}", message, smsMessage);
            }

            String encodingInfo = YorubaTextUtil.getSmsEncodingInfo(smsMessage);
            log.info("Sending SMS to {} | {} | Message: {}", phoneNumber, encodingInfo, smsMessage);

            String[] recipients = new String[]{phoneNumber};
            String from = africasTalkingProperties.getSenderId();

            log.info("Calling Africa's Talking SMS API - From: {}, To: {}", from, phoneNumber);

            List<Recipient> response = smsService.send(smsMessage, from, recipients, true);

            if (response != null && !response.isEmpty()) {
                for (Recipient recipient : response) {
                    log.info("SMS Result - Number: {}, Status: {}, MessageId: {}, Cost: {}",
                            recipient.number,
                            recipient.status,
                            recipient.messageId,
                            recipient.cost);

                    if ("Success".equalsIgnoreCase(recipient.status)) {
                        log.info("SMS sent successfully to {} ({})",
                                recipient.number, encodingInfo);
                    } else {
                        log.error("SMS failed to {}: {}", recipient.number, recipient.status);
                    }
                }
            } else {
                log.warn("No response received from Africa's Talking for SMS to {}", phoneNumber);
            }

        } catch (Exception ex) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, ex.getMessage(), ex);
        }
    }
}