package com.taxhelp.nigerian_tax_ussd.model.request;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UssdRequest {
    private String sessionId;
    private String serviceCode;
    private String phoneNumber;
    private String text;


    public boolean isInitialRequest(){
        return text == null || text.isEmpty();
    }

    public String getLatestInput(){
        if (text == null || text.isEmpty()) {
            return "";
        }
            String[] parts = text.split("\\*");
            return parts[parts.length - 1];

    }
}
