package org.example.containerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class ContainerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContainerServiceApplication.class, args);
    }
}
