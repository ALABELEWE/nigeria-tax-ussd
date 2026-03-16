package com.taxhelp.nigerian_tax_ussd.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for evaluating RAG response quality
 */
@Service
@Slf4j
public class ResponseEvaluationService {

    private static final int MIN_RESPONSE_LENGTH = 20;
    private static final int MAX_RESPONSE_LENGTH = 300;
    private static final int OPTIMAL_RESPONSE_LENGTH = 160;

    public EvaluationResult evaluateResponse(String question, String response) {
        EvaluationResult result = new EvaluationResult();

        if (response == null || response.trim().isEmpty()) {
            result.setQualityScore(0.0);
            result.setRelevant(false);
            result.addIssue("Empty response");
            return result;
        }

        double score = 0.0;

        // 1. Length appropriateness (max 20 points)
        score += evaluateLength(response, result);

        // 2. Relevance indicators (max 30 points)
        score += evaluateRelevance(question, response, result);

        // 3. Completeness (max 20 points)
        score += evaluateCompleteness(response, result);

        // 4. Clarity (max 15 points)
        score += evaluateClarity(response, result);

        // 5. Safety (max 15 points) — must run before setting relevant
        score += evaluateSafety(response, result);

        // Normalize score to 0.0 - 1.0
        result.setQualityScore(Math.max(0, Math.min(100, score)) / 100.0);

        // Only set relevant based on score if safety check didn't already reject it
        if (!result.isInappropriateContent()) {
            result.setRelevant(result.getQualityScore() >= 0.6);
        }

        log.debug("Response evaluation - Score: {}, Relevant: {}, Issues: {}",
                String.format("%.2f", result.getQualityScore() * 100),
                result.isRelevant(),
                result.getIssues().size()
        );

        return result;
    }

    private double evaluateLength(String response, EvaluationResult result) {
        int length = response.length();

        if (length < MIN_RESPONSE_LENGTH) {
            result.addIssue("Response too short");
            return -15.0;
        }

        if (length > MAX_RESPONSE_LENGTH) {
            result.addIssue("Response too long for SMS");
            return -10.0;
        }

        if (length <= OPTIMAL_RESPONSE_LENGTH) {
            return 20.0; // Full points for optimal length
        }

        return 10.0; // Partial points for acceptable but non-optimal length
    }

    private double evaluateRelevance(String question, String response, EvaluationResult result) {
        double score = 0.0;

        String[] taxKeywords = {"tax", "vat", "cit", "paye", "rate", "exempt", "revenue",
                "firs", "naira", "income", "corporate", "withholding"};

        int keywordMatches = 0;
        for (String keyword : taxKeywords) {
            if (response.toLowerCase().contains(keyword)) {
                keywordMatches++;
            }
        }

        if (keywordMatches > 0) {
            score += Math.min(20.0, keywordMatches * 5.0);
        } else {
            result.addIssue("No tax-related keywords found");
        }

        if (question.toLowerCase().contains("what") || question.toLowerCase().contains("kini")) {
            if (response.toLowerCase().contains("is") || response.toLowerCase().contains("are")) {
                score += 5.0;
            }
        }

        if (question.toLowerCase().contains("how") || question.toLowerCase().contains("bawo")) {
            if (response.toLowerCase().contains("by") || response.toLowerCase().contains("to")) {
                score += 5.0;
            }
        }

        return score;
    }

    private double evaluateCompleteness(String response, EvaluationResult result) {
        double score = 0.0;

        if (response.trim().endsWith(".") || response.trim().endsWith("!")) {
            score += 5.0;
        }

        if (response.matches(".*\\d+.*")) {
            score += 10.0;
        }

        if (response.contains(",") || response.contains(";")) {
            score += 5.0;
        }

        return score;
    }

    private double evaluateClarity(String response, EvaluationResult result) {
        double score = 0.0;

        String[] words = response.split("\\s+");
        int longWords = 0;
        for (String word : words) {
            if (word.length() > 12) {
                longWords++;
            }
        }

        if (longWords > words.length * 0.3) {
            result.addIssue("Too much technical jargon");
            return -5.0;
        }

        if (words.length > 0) {
            double avgWordLength = response.replace(" ", "").length() / (double) words.length;
            if (avgWordLength < 6.0) {
                score += 10.0;
            }
        }

        if (response.toLowerCase().contains("naira") ||
                response.toLowerCase().contains("nigeria") ||
                response.toLowerCase().contains("firs")) {
            score += 5.0;
        }

        return score;
    }

    private double evaluateSafety(String response, EvaluationResult result) {
        double score = 0.0;

        if (response.toLowerCase().contains("may") ||
                response.toLowerCase().contains("generally") ||
                response.toLowerCase().contains("typically")) {
            score += 5.0;
        }

        if (response.toLowerCase().contains("always") ||
                response.toLowerCase().contains("never") ||
                response.toLowerCase().contains("must")) {
            result.addIssue("Contains absolute statements");
            score -= 5.0;
        }

        String[] inappropriateWords = {"hack", "evade", "cheat", "illegal", "fraud"};
        for (String word : inappropriateWords) {
            if (response.toLowerCase().contains(word)) {
                result.addIssue("Contains inappropriate content");
                result.setInappropriateContent(true);
                result.setRelevant(false);
                return -20.0; // Early return, no base score
            }
        }

        return score + 10.0;
    }

    public static class EvaluationResult {
        private double qualityScore = 0.0;
        private boolean relevant = false;
        private boolean inappropriateContent = false;
        private java.util.List<String> issues = new java.util.ArrayList<>();

        public double getQualityScore() { return qualityScore; }
        public void setQualityScore(double qualityScore) { this.qualityScore = qualityScore; }

        public boolean isRelevant() { return relevant; }
        public void setRelevant(boolean relevant) { this.relevant = relevant; }

        public boolean isInappropriateContent() { return inappropriateContent; }
        public void setInappropriateContent(boolean inappropriateContent) {
            this.inappropriateContent = inappropriateContent;
        }

        public java.util.List<String> getIssues() { return issues; }
        public void addIssue(String issue) { issues.add(issue); }
        public boolean hasIssues() { return !issues.isEmpty(); }
    }
}