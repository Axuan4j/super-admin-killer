package com.sak.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.sak.service.mapper")
@EnableAsync
public class SuperAdminKillerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuperAdminKillerApiApplication.class, args);
    }
}
