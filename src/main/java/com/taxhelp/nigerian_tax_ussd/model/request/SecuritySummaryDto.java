package com.taxhelp.nigerian_tax_ussd.model.request;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SecuritySummaryDto {
    private long totalEvents;
    private long last24Hours;
    private Map<String, Long> byType;
    private Map<String, Long> bySeverity;
    private List<TopOffenderDto> topOffenders;
}