package com.taxhelp.nigerian_tax_ussd.model.request;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class SecurityEventFilter {
    private String eventType;
    private String severity;
    private LocalDate from;
    private LocalDate to;
}