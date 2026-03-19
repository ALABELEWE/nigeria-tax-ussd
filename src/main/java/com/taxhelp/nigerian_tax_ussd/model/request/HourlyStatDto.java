package com.taxhelp.nigerian_tax_ussd.model.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HourlyStatDto {
    private String hour;
    private long sessions;
}