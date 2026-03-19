package com.taxhelp.nigerian_tax_ussd.model.request;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class TopOffenderDto {
    private String phoneNumber;
    private long attempts;
    private LocalDateTime lastAttempt;
    private boolean blocked;
}