package com.taxhelp.nigerian_tax_ussd.model.request;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagQueryRequest {
    private String question;
    @Builder.Default
    private Integer maxLength = 140;
}
