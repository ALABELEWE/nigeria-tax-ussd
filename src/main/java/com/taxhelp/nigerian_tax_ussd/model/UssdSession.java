//package com.taxhelp.nigerian_tax_ussd.model;
//
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.redis.core.RedisHash;
//import org.springframework.data.redis.core.TimeToLive;
//
//import java.io.Serializable;
//
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//@RedisHash("ussd_sessions")
//public class UssdSession implements Serializable {
//    @Id
//    private String sessionId;
//    private String phoneNumber;
//    private String currentStep;  //LANGUAGE_SELECTION, QUESTION_INPUT, PROCESSING
//    private String question;
//    private Long createdAt;
//
//
//    @TimeToLive
//    private Long ttl = 300L;
//
//}
