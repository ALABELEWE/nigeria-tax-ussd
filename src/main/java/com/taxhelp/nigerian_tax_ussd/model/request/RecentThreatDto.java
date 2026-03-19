package com.taxhelp.nigerian_tax_ussd.model.request;

import com.taxhelp.nigerian_tax_ussd.model.SecurityEvent;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RecentThreatDto {
    private String eventType;
    private SecurityEvent.Severity severity;
    private boolean blocked;
    private LocalDateTime timestamp;

    public static RecentThreatDto from(SecurityEvent e) {
        return RecentThreatDto.builder()
                .eventType(e.getEventType())
                .severity(e.getSeverity())
                .blocked(e.isBlocked())
                .timestamp(e.getTimestamp())
                .build();
    }
}