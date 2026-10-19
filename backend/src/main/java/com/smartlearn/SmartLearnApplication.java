package com.smartlearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@org.springframework.scheduling.annotation.EnableScheduling
@SpringBootApplication
public class SmartLearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartLearnApplication.class, args);
    }
}
