package com.hrms;

import com.hrms.config.OfferPdfTemplateProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(OfferPdfTemplateProperties.class)
public class HrmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrmsApplication.class, args);
    }
}
