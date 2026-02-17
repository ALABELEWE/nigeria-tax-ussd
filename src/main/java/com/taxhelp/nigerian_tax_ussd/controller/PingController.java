package com.taxhelp.nigerian_tax_ussd.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class PingController {

    @GetMapping("/")
    public String home() {
        return "Nigerian Tax USSD Service Started!";
    }

    @GetMapping("/ping")
    public String ping() {
        return "PONG - Service is alive at " + LocalDateTime.now();
    }
}
