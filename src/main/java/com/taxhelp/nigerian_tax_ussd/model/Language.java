package com.taxhelp.nigerian_tax_ussd.model;


import lombok.Getter;

@Getter
public class Language {
    private final String code;
    private final String name;
    private final String welcome;

    public Language(String code, String name, String welcome) {
        this.code = code;
        this.name = name;
        this.welcome = welcome;
    }
}
