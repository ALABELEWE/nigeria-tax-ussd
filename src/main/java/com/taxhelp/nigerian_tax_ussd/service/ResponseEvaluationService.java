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
    private static final int OPTIMAL_RESPONSE_LENGTH = 160; // SMS-friendly

    /**
     * Evaluate response quality
     * 
     * @param question Original question
     * @param response RAG response
     * @return Evaluation result with quality score
     */
    public EvaluationResult evaluateResponse(String question, String response) {
        EvaluationResult result = new EvaluationResult();
        
        if (response == null || response.trim().isEmpty()) {
            result.setQualityScore(0.0);
            result.setRelevant(false);
            result.addIssue("Empty response");
            return result;
        }

        double score = 100.0;
        
        // 1. Length appropriateness (20 points)
        score += evaluateLength(response, result);
        
        // 2. Relevance indicators (30 points)
        score += evaluateRelevance(question, response, result);
        
        // 3. Completeness (20 points)
        score += evaluateCompleteness(response, result);
        
        // 4. Clarity (15 points)
        score += evaluateClarity(response, result);
        
        // 5. Safety (15 points)
        score += evaluateSafety(response, result);
        
        // Normalize score to 0-100
        result.setQualityScore(Math.max(0, Math.min(100, score)) / 100.0);
        
        // Determine if relevant (threshold: 60%)
        result.setRelevant(result.getQualityScore() >= 0.6);
        
        log.debug("Response evaluation - Score: {}, Relevant: {}, Issues: {}", 
            String.format("%.2f", result.getQualityScore() * 100),
            result.isRelevant(),
            result.getIssues().size()
        );
        
        return result;
    }

    /**
     * Evaluate response length appropriateness
     */
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
        
        // Optimal length bonus
        if (length <= OPTIMAL_RESPONSE_LENGTH) {
            return 5.0;
        }
        
        return 0.0;
    }

    /**
     * Evaluate relevance to question
     */
    private double evaluateRelevance(String question, String response, EvaluationResult result) {
        double score = 0.0;
        
        // Check for key tax terms
        String[] taxKeywords = {"tax", "vat", "cit", "paye", "rate", "exempt", "revenue", 
                               "firs", "naira", "income", "corporate", "withholding"};
        
        int keywordMatches = 0;
        for (String keyword : taxKeywords) {
            if (response.toLowerCase().contains(keyword)) {
                keywordMatches++;
            }
        }
        
        if (keywordMatches > 0) {
            score += Math.min(15.0, keywordMatches * 5.0);
        } else {
            result.addIssue("No tax-related keywords found");
        }
        
        // Check if response addresses the question type
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

    /**
     * Evaluate completeness
     */
    private double evaluateCompleteness(String response, EvaluationResult result) {
        double score = 0.0;
        
        // Check for complete sentences
        if (response.trim().endsWith(".") || response.trim().endsWith("!")) {
            score += 5.0;
        }
        
        // Check for specific information (numbers, percentages)
        if (response.matches(".*\\d+.*")) {
            score += 10.0; // Contains numbers (likely rates or amounts)
        }
        
        // Check for proper structure
        if (response.contains(",") || response.contains(";")) {
            score += 5.0; // Contains detailed information
        }
        
        return score;
    }

    /**
     * Evaluate clarity
     */
    private double evaluateClarity(String response, EvaluationResult result) {
        double score = 0.0;
        
        // Check for excessive jargon (too many long words)
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
        
        // Check for clear language
        if (words.length > 0) {
            double avgWordLength = response.replace(" ", "").length() / (double) words.length;
            if (avgWordLength < 6.0) {
                score += 10.0; // Simple, clear language
            }
        }
        
        // Bonus for Nigerian context
        if (response.toLowerCase().contains("naira") || 
            response.toLowerCase().contains("nigeria") ||
            response.toLowerCase().contains("firs")) {
            score += 5.0;
        }
        
        return score;
    }

    /**
     * Evaluate safety (no harmful content)
     */
    private double evaluateSafety(String response, EvaluationResult result) {
        double score = 0.0;
        
        // Check for disclaimers or uncertainty markers
        if (response.toLowerCase().contains("may") || 
            response.toLowerCase().contains("generally") ||
            response.toLowerCase().contains("typically")) {
            score += 5.0; // Appropriately cautious
        }
        
        // Penalize absolute statements without context
        if (response.toLowerCase().contains("always") || 
            response.toLowerCase().contains("never") ||
            response.toLowerCase().contains("must")) {
            result.addIssue("Contains absolute statements");
            return -5.0;
        }
        
        // Check for inappropriate content
        String[] inappropriateWords = {"hack", "evade", "cheat", "illegal", "fraud"};
        for (String word : inappropriateWords) {
            if (response.toLowerCase().contains(word)) {
                result.addIssue("Contains inappropriate content");
                result.setRelevant(false);
                return -20.0;
            }
        }
        
        return score + 10.0; // Base safety score
    }

    /**
     * Evaluation result class
     */
    public static class EvaluationResult {
        private double qualityScore = 0.0; // 0.0 to 1.0
        private boolean relevant = false;
        private java.util.List<String> issues = new java.util.ArrayList<>();
        
        public double getQualityScore() {
            return qualityScore;
        }
        
        public void setQualityScore(double qualityScore) {
            this.qualityScore = qualityScore;
        }
        
        public boolean isRelevant() {
            return relevant;
        }
        
        public void setRelevant(boolean relevant) {
            this.relevant = relevant;
        }
        
        public java.util.List<String> getIssues() {
            return issues;
        }
        
        public void addIssue(String issue) {
            issues.add(issue);
        }
        
        public boolean hasIssues() {
            return !issues.isEmpty();
        }
    }
}