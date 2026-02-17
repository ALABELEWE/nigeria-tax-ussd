package com.taxhelp.nigerian_tax_ussd.service;


import com.taxhelp.nigerian_tax_ussd.model.response.RagQueryResponse;


public interface RagClientService {
    RagQueryResponse queryTaxAssistant(String question);
}
