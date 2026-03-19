package com.taxhelp.nigerian_tax_ussd.model.request;

import com.taxhelp.nigerian_tax_ussd.model.SecurityEvent;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SecurityEventDto {
    private Long id;
    private String sessionId;
    private String eventType;
    private SecurityEvent.Severity severity;
    private String description;
    private String inputSample;
    private boolean blocked;
    private LocalDateTime timestamp;

    public static SecurityEventDto from(SecurityEvent e) {
        return SecurityEventDto.builder()
                .id(e.getId())
                .sessionId(e.getSessionId())
                .eventType(e.getEventType())
                .severity(e.getSeverity())
                .description(e.getDescription())
                .inputSample(e.getInputSample())
                .blocked(e.isBlocked())
                .timestamp(e.getTimestamp())
                .build();
    }
}