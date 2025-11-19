package com.example.tcgbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TcgArenaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcgArenaBackendApplication.class, args);
    }

}