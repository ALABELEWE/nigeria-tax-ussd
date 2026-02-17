package com.taxhelp.nigerian_tax_ussd.service.impl;

import com.taxhelp.nigerian_tax_ussd.config.RagServiceProperties;
import com.taxhelp.nigerian_tax_ussd.model.request.RagQueryRequest;
import com.taxhelp.nigerian_tax_ussd.model.response.RagQueryResponse;
import com.taxhelp.nigerian_tax_ussd.service.RagClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagClientServiceImpl implements RagClientService {

    private final WebClient ragWebClient;
    private final RagServiceProperties ragProperties;

    @Override
    public RagQueryResponse queryTaxAssistant(String question) {
        log.debug("Querying RAG service with question {}", question);

        RagQueryRequest request = RagQueryRequest.builder()
                .question(question)
                .maxLength(ragProperties.getMaxLength())
                .build();

        try{
            RagQueryResponse response = ragWebClient
                    .post()
                    .uri(ragProperties.getQueryEndpoint())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(RagQueryResponse.class)
                    .timeout(Duration.ofMillis(ragProperties.getTimeout()))
                    .block();
            log.debug("RAG service returned {}", response);
            return response;
        }catch(Exception e){
            log.error("Error calling RAG service",e);

            RagQueryResponse errorResponse = new RagQueryResponse();
            errorResponse.setAnswer("Sorry, I am having trouble processing your request. Please try again.");
            errorResponse.setSuccess(false);
            errorResponse.setError(e.getMessage());
            return errorResponse;
        }
    }
}
