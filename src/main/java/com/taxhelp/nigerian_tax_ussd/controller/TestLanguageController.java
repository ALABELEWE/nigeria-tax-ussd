package com.taxhelp.nigerian_tax_ussd.controller;


import com.taxhelp.nigerian_tax_ussd.config.GoogleTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestLanguageController {

//    private final TranslationService translationService;

    private final GoogleTranslationService googleTranslationService;

    @GetMapping("/translate")
    public Map<String, String> testTranslation(
            @RequestParam String text,
            @RequestParam(defaultValue = "en") String from,
            @RequestParam(defaultValue = "yo") String to
    ){
        String translated = googleTranslationService.translate(text, from, to);

        Map<String,String> map = new HashMap<>();
        map.put("original",text);
        map.put("translated",translated);
        map.put("from",from);
        map.put("to",to);

        return map;
    }
}
