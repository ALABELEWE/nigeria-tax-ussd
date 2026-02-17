package com.taxhelp.nigerian_tax_ussd.config;

import com.taxhelp.nigerian_tax_ussd.model.Language;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LanguageConfig {

    private final Map<String, Language> languages = new HashMap<>();
    public LanguageConfig() {
        // Initialize supported languages
        languages.put("1", new Language("en", "English", "Welcome to Nigeria Tax Help"));
        languages.put("2", new Language("yo", "Yoruba", "Ẹ káàbọ̀ sí Ìrànwọ́ Owó-Orí Nàìjíríà"));
        languages.put("3", new Language("ig","Igbo", "Nnọọ na Enyemaka Ụtụ Naịjirịa"));
        languages.put("4", new Language("ha", "Hausa", "Barka da zuwa Taimakon Haraji ta Najeriya"));
    }

    public Language getLanguageByOption(String option) {
        return languages.get(option);
    }

    public String getWelcomeMessage(String lanuageCode){
        return languages.values().stream()
                .filter(l -> l.getCode().equals(lanuageCode))
                .findFirst()
                .map(Language::getWelcome)
                .orElse("Welcome to Nigeria Tax Help");
    }
    public Map<String, Language> getAllLanguages() {
        return new HashMap<>(languages);
    }

}
