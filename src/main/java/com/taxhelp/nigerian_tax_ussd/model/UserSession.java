package com.taxhelp.nigerian_tax_ussd.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String phoneNumber;
    private String language;
    private SessionStage stage;
    private Instant createdAt;
    private Instant lastAccessedAt;

    public enum SessionStage {
        INITIAL,
        LANGUAGE_SELECTED,
        AWAITING_QUESTION,
        PROCESSING,
    }
}
