//package com.taxhelp.nigerian_tax_ussd.service;
//
//
//import com.google.cloud.translate.Translate;
//import com.google.cloud.translate.TranslateOptions;
//import com.google.cloud.translate.Translation;
//import jakarta.annotation.PostConstruct;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//public class TranslationService {
//
//    @Value("${google.api.key}")
//    private String apiKey;
//
//    private Translate translate;
//
//    @PostConstruct
//    public void init(){
//        try{
//            translate = TranslateOptions.newBuilder()
//                            .setProjectId("at-project-486416")
//                                    .build()
//                                            .getService();
//            System.setProperty("GOOGLE_API_KEY", apiKey);
//            log.info("Google Translation Service initialized successfully.");
//        }catch(Exception e){
//            log.error("Failed to initialize Google Translation service",e);
//        }
//    }
//
//    public String translate(String text, String sourceLanguage, String targetLanguage){
//        try{
//            log.info("Translating: '{}' from '{}' to '{}'", text, sourceLanguage, targetLanguage);
//
//            // If same language, return as-is
//            if(sourceLanguage.equalsIgnoreCase(targetLanguage)){
//                return text;
//            }
//            Translation translation = translate.translate(
//                    text,
//                    Translate.TranslateOption.sourceLanguage(sourceLanguage),
//                    Translate.TranslateOption.targetLanguage(targetLanguage)
//            );
//
//            String translatedText = translation.getTranslatedText();
//            log.info("Translation result: '{}'", translatedText);
//
//            return translatedText;
//        }catch(Exception e){
//            log.error("Failed to translate text",e);
//            return text;  //Return original text if translation fails
//        }
//    }
//
//    public String detectLanguage(String text){
//        try{
//            com.google.cloud.translate.Detection detection = translate.detect(text);
//            String detectedLanguage = detection.getLanguage();
//
//            log.info("Detected language: {} for text: '{}'", detectedLanguage, text);
//            return detectedLanguage;
//        }catch(Exception e){
//            log.error("Failed to translate text",e);
//            return "en"; //Default to English
//        }
//    }
//}
