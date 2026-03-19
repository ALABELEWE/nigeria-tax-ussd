package com.taxhelp.nigerian_tax_ussd.model.request;

import com.taxhelp.nigerian_tax_ussd.model.QuestionLog;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class LowScoreQuestionDto {
    private Long id;
    private String question;
    private Double qualityScore;
    private Boolean isRelevant;
    private String language;
    private String evaluationNotes;
    private LocalDateTime timestamp;

    public static LowScoreQuestionDto from(QuestionLog q) {
        return LowScoreQuestionDto.builder()
                .id(q.getId())
                .question(q.getQuestion())
                .qualityScore(q.getQualityScore())
                .isRelevant(q.getIsRelevant())
                .language(q.getLanguage())
                .evaluationNotes(q.getEvaluationNotes())
                .timestamp(q.getTimestamp())
                .build();
    }
}