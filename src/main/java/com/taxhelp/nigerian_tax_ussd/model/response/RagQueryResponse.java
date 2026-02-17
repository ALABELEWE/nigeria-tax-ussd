package com.taxhelp.nigerian_tax_ussd.model.response;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
public class RagQueryResponse {
    private String answer;
    private Boolean success;
    private String chunksFound;
    private String error;
}
