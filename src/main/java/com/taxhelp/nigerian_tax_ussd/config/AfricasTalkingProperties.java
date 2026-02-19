package com.taxhelp.nigerian_tax_ussd.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "africastalking")
public class AfricasTalkingProperties {
    private String username = "sandbox";
    private String apiKey = "atsk_b7393a0766a5ff7ef60d85dc00dbfd098554cb45178b65465a9e4274f14327753a2fb888";
    private String smsUrl = "https://api.sandbox.africastalking.com/version1/messaging";
    private String shortcode ="*384*36903#";
    private String senderId = "12096";

}