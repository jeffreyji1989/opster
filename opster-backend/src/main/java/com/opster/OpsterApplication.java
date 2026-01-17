package com.opster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OpsterApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpsterApplication.class, args);
    }

}
