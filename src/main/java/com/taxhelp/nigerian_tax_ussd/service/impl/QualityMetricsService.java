package com.taxhelp.nigerian_tax_ussd.service.impl;

import com.taxhelp.nigerian_tax_ussd.model.request.LowScoreQuestionDto;
import com.taxhelp.nigerian_tax_ussd.model.request.QualityMetricsDto;
import com.taxhelp.nigerian_tax_ussd.repository.QuestionLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityMetricsService {

    private final QuestionLogRepository questionLogRepository;

    public QualityMetricsDto getMetrics() {
        Double avg    = questionLogRepository.findAverageQualityScore();
        Double median = questionLogRepository.findMedianQualityScore();
        long   total  = questionLogRepository.countByQualityScoreIsNotNull();
        long relevant = questionLogRepository.countByIsRelevantTrue();
        long   below  = questionLogRepository.countByQualityScoreLessThan(0.5);

        Map<String, Long> dist = new LinkedHashMap<>();
        dist.put("0.8-1.0", questionLogRepository.countByQualityScoreBetween(0.8, 1.0));
        dist.put("0.6-0.8", questionLogRepository.countByQualityScoreBetween(0.6, 0.8));
        dist.put("0.4-0.6", questionLogRepository.countByQualityScoreBetween(0.4, 0.6));
        dist.put("0.2-0.4", questionLogRepository.countByQualityScoreBetween(0.2, 0.4));
        dist.put("0.0-0.2", questionLogRepository.countByQualityScoreBetween(0.0, 0.2));

        double relevanceRate = total > 0 ? (double) relevant / total : 0.0;

        return QualityMetricsDto.builder()
                .averageQualityScore(avg    != null ? avg    : 0.0)
                .medianQualityScore(median  != null ? median : 0.0)
                .relevanceRate(relevanceRate)
                .belowThresholdCount(below)
                .totalQuestionsEvaluated(total)
                .scoreDistribution(dist)
                .build();
    }

    public Page<LowScoreQuestionDto> getLowScores(double threshold, int limit) {
        return questionLogRepository
                .findByQualityScoreLessThanOrderByQualityScoreAsc(threshold, PageRequest.of(0, limit))
                .map(LowScoreQuestionDto::from);
    }
}