package com.taxhelp.nigerian_tax_ussd.model.request;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class QualityMetricsDto {
    private double averageQualityScore;
    private double medianQualityScore;
    private double relevanceRate;
    private long belowThresholdCount;
    private long totalQuestionsEvaluated;
    private Map<String, Long> scoreDistribution;
}