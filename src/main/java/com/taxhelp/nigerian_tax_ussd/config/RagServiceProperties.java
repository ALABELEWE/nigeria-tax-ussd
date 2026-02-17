package com.taxhelp.nigerian_tax_ussd.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "rag.service")
public class RagServiceProperties {
    private String url = "http://localhost:8000";
    private String queryEndpoint = "/api/v1/query";
    private Integer timeout = 60000;
    private Integer maxLength = 140;
}
