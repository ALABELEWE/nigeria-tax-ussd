package com.taxhelp.nigerian_tax_ussd.controller;


import com.taxhelp.nigerian_tax_ussd.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SessionService sessionService2;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();

        // Check Redis connection
        try{
            redisTemplate.getConnectionFactory().getConnection().ping();
            health.put("redis", "UP");
        }catch(Exception e){
            health.put("redis", "DOWN");
            health.put("redis_error", e.getMessage());
        }
        // Session stats
        health.put("active_sessions", sessionService2.getActiveSessionCount());
        health.put("status", "UP");

        return health;
    }
}
