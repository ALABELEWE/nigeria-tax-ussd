package com.taxhelp.nigerian_tax_ussd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class NigerianTaxUssdApplication {

	public static void main(String[] args) {
		SpringApplication.run(NigerianTaxUssdApplication.class, args);
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Nigerian Tax USSD Service Started!");
        System.out.println("USSD Endpoint: http://localhost:8080/ussd/callback");
        System.out.println("Health Check: http://localhost:8080/health");
        System.out.println("Test RAG: http://localhost:8080/test/rag?question=What%20is%20VAT");
        System.out.println("Redis: localhost:6379");
        System.out.println("=".repeat(60) + "\n");

	}

}
