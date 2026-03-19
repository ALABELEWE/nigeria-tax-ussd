package com.taxhelp.nigerian_tax_ussd.model.request;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class DailyStatDto {
    private LocalDate date;
    private long sessions;
    private long questions;
    private double avgQuality;
    private long avgResponseMs;
    private double successRate;
}