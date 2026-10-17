package dev.roastapp.roast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {"dev.roastapp.roast", "dev.roastapp.common"})
@EnableAsync
public class RoastServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoastServiceApplication.class, args);
    }
}
