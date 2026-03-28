package com.hybriddependcyanlysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.hybriddependcyanlysis", "Common.JWT"})
public class HybridDependcyAnlysisApplication {
    public static void main(String[] args) {
        SpringApplication.run(HybridDependcyAnlysisApplication.class, args);
    }

}
