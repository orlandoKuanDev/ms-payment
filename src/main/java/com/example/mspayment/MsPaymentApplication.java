package com.example.mspayment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@EnableEurekaClient
@SpringBootApplication
public class MsPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsPaymentApplication.class, args);
    }

}
