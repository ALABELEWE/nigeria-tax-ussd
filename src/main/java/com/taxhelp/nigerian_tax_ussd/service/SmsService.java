package com.taxhelp.nigerian_tax_ussd.service;

public interface SmsService {
    void sendSmsAsync(String phoneNumber, String message);
}
