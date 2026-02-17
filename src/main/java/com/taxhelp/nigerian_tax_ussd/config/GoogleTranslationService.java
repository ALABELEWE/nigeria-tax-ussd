package com.taxhelp.nigerian_tax_ussd.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GoogleTranslationService {


    @Value("${google.api.key}")
    private String apiKey;

    private final WebClient webClient;

    public GoogleTranslationService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://translation.googleapis.com/")
                .build();
    }

    public String translate(String text, String sourceLanguage, String targetLanguage) {
        try{
            if(sourceLanguage.equalsIgnoreCase(targetLanguage)){
                return text;
            }

            log.info("Translating from {} to {}: '{}'", sourceLanguage, targetLanguage, text);

            Map<String, Object> response = webClient
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/language/translate/v2")
                            .queryParam("key", apiKey)
                            .queryParam("q", text)
                            .queryParam("source", sourceLanguage)
                            .queryParam("target", targetLanguage)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

// Extract translated text from response
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> translations = (List<Map<String, Object>>) data.get("translations");
            String translatedText = (String) translations.get(0).get("translatedText");

            log.info("Translated from {} to {}: {}", sourceLanguage, targetLanguage, translatedText);
            return translatedText;
        }catch (Exception e){
            log.error("Error while translating text", e);
            return text;
        }
    }

    public String detectLanguage(String text) {
        try{
            Map<String, Object> response = webClient
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/language/translate/v2/detect")
                            .queryParam("key", apiKey)
                            .queryParam("q", text)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            List<Map<String, Object>> translations = (List<Map<String, Object>>) data.get("translations");
            String language = (String) translations.get(0).get("language");

            log.info("Detected language: {}", language);
            return language;
        }catch (Exception e){
            log.error("Error while detecting language", e);
            return "en";
        }
    }
}
