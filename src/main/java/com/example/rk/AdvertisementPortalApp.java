package com.example.rk;

import com.example.rk.config.ApplicationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
public class AdvertisementPortalApp {
    public static void main(String[] args) {
        SpringApplication.run(AdvertisementPortalApp.class);
    }
}
