package com.clinicaapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableMongoRepositories("com.clinicaapp.repository")
@EnableScheduling
public class ClinicaappApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicaappApplication.class, args);
    }

}