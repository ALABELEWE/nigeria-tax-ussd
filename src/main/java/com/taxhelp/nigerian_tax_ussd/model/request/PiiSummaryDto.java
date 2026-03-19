package com.taxhelp.nigerian_tax_ussd.model.request;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class PiiSummaryDto {
    private long totalIncidents;
    private long last24Hours;
    private double redactionRate;
    private Map<String, Long> byType;
    private double incidentRate;
}