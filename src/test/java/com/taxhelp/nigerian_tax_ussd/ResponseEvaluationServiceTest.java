package com.taxhelp.nigerian_tax_ussd;

import com.taxhelp.nigerian_tax_ussd.service.ResponseEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ResponseEvaluationServiceTest {

    @Autowired
    private ResponseEvaluationService evaluationService;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testGoodResponse() {
        String question = "What is the VAT rate in Nigeria?";
        String response = "The VAT rate in Nigeria is 7.5%. It applies to most goods and services.";
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertTrue(result.isRelevant());
        assertTrue(result.getQualityScore() > 0.6);
    }

    @Test
    void testTooShortResponse() {
        String question = "What is VAT?";
        String response = "A tax.";
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().contains("Response too short"));
    }

    @Test
    void testTooLongResponse() {
        String question = "What is VAT?";
        String response = "a".repeat(400);
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().contains("Response too long for SMS"));
    }

    @Test
    void testEmptyResponse() {
        String question = "What is VAT?";
        String response = "";
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertFalse(result.isRelevant());
        assertEquals(0.0, result.getQualityScore());
    }

    @Test
    void testNullResponse() {
        String question = "What is VAT?";
        String response = null;
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertFalse(result.isRelevant());
        assertEquals(0.0, result.getQualityScore());
    }

    @Test
    void testResponseWithNumbers() {
        String question = "What is the CIT rate?";
        String response = "The Corporate Income Tax rate is 30% for companies in Nigeria.";
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertTrue(result.isRelevant());
        assertTrue(result.getQualityScore() > 0.7);
    }

    @Test
    void testResponseWithInappropriateContent() {
        String question = "How to reduce tax?";
        String response = "You can evade tax by hiding your income from FIRS.";
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertFalse(result.isRelevant());
        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> i.contains("inappropriate content")));
    }

    @Test
    void testResponseWithTaxKeywords() {
        String question = "Tell me about PAYE";
        String response = "PAYE stands for Pay As You Earn. It is a withholding tax on employment income in Nigeria.";
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertTrue(result.isRelevant());
        assertTrue(result.getQualityScore() > 0.6);
    }

    @Test
    void testResponseWithoutTaxKeywords() {
        String question = "What is VAT?";
        String response = "It's something you need to know about.";
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertTrue(result.hasIssues());
        assertTrue(result.getIssues().stream()
            .anyMatch(i -> i.contains("No tax-related keywords")));
    }

    @Test
    void testYorubaQuestion() {
        String question = "Kí ni oṣùwọ̀n VAT?";
        String response = "Oṣùwọ̀n VAT ni 7.5% ni Nigeria.";
        
        var result = evaluationService.evaluateResponse(question, response);
        
        // Should still evaluate even for non-English
        assertNotNull(result);
        assertTrue(result.getQualityScore() >= 0);
    }

    @Test
    void testOptimalLength() {
        String question = "What is the VAT rate?";
        String response = "The VAT rate in Nigeria is 7.5%.";
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertTrue(result.isRelevant());
        // Optimal length should get bonus points
        assertTrue(result.getQualityScore() > 0.5);
    }

    @Test
    void testResponseWithNigerianContext() {
        String question = "What is CIT?";
        String response = "CIT is Corporate Income Tax. In Nigeria, it's managed by FIRS and the rate is 30%.";
        
        var result = evaluationService.evaluateResponse(question, response);
        
        assertTrue(result.isRelevant());
        assertTrue(result.getQualityScore() > 0.7);
    }
}