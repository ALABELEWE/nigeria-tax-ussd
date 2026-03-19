package com.taxhelp.nigerian_tax_ussd.model.request;

import com.taxhelp.nigerian_tax_ussd.model.QuestionLog;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class PiiIncidentDto {
    private Long id;
    private String sessionId;
    private List<String> detectedTypes;
    private String language;
    private boolean redacted;
    private LocalDateTime timestamp;

    public static PiiIncidentDto from(QuestionLog q) {
        List<String> types = new ArrayList<>();
        String notes = q.getEvaluationNotes();
        if (notes != null) {
            if (notes.contains("PHONE")) types.add("PHONE_NUMBER");
            if (notes.contains("TIN"))   types.add("TIN");
            if (notes.contains("BVN"))   types.add("BVN");
            if (notes.contains("EMAIL")) types.add("EMAIL");
        }
        return PiiIncidentDto.builder()
                .id(q.getId())
                .sessionId(q.getSessionId())
                .detectedTypes(types)
                .language(q.getLanguage())
                .redacted(true)
                .timestamp(q.getTimestamp())
                .build();
    }
}