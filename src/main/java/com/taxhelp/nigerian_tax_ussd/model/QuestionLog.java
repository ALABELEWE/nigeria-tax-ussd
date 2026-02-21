package com.taxhelp.nigerian_tax_ussd.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name= "question_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private  String phoneNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private  String question;

    @Column(columnDefinition = "TEXT")
    private  String answer;

    @Column(nullable = false, length = 5)
    private  String language;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Boolean smsDelivered;

    @Column
    private Integer responseTimeMs;

    @Column(length = 100)
    private String sessionId;

    public QuestionLog(String phoneNumber, String question, String answer,
                       String language, LocalDateTime timestamp, Boolean smsDelivered) {
        this.phoneNumber = phoneNumber;
        this.question = question;
        this.answer = answer;
        this.language = language;
        this.timestamp = timestamp;
        this.smsDelivered = smsDelivered;
    }
}
