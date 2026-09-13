package com.packersmovers.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;



@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class PackersMoversApplication {

    public static void main(String[] args) {
        SpringApplication.run(PackersMoversApplication.class, args);
    }
}
