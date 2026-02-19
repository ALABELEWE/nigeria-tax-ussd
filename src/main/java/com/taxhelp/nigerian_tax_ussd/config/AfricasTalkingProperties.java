package com.taxhelp.nigerian_tax_ussd.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "africastalking")
public class AfricasTalkingProperties {
    private String username;
    private String apiKey;
    private String smsUrl;
    private String shortcode;
    private String senderId;
}