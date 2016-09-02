package com.cmpwrn.controller;

import org.joda.time.DateTimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DemoApi {

    public static void main(String[] args) {
        DateTimeZone.setDefault(DateTimeZone.UTC);
        SpringApplication.run(DemoApi.class, args);
    }
}

