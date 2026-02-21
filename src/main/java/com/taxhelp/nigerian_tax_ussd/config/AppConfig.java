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


}
