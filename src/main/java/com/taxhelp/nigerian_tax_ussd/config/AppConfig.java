package com.taxhelp.nigerian_tax_ussd.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AppConfig {

    @Bean
    public WebClient ragWebClient(RagServiceProperties ragProperties) {
        return WebClient.builder()
                .baseUrl(ragProperties.getUrl())
                .build();
    }

//    @Bean
//    public WebClient smsWebClient(AfricasTalkingProperties africasTalkingProperties) {
//        return WebClient.builder()
//                .baseUrl(africasTalkingProperties.getSmsUrl())
//                .defaultHeader("apiKey", africasTalkingProperties.getApiKey())
//                .defaultHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
//                .build();
//
//    }


}
