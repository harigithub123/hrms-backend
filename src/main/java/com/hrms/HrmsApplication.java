package com.hrms;

import com.hrms.config.HrmsEmailProperties;
import com.hrms.config.OfferPdfTemplateProperties;
import com.hrms.config.PayrollStatutoryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        OfferPdfTemplateProperties.class,
        HrmsEmailProperties.class,
        PayrollStatutoryProperties.class
})
public class HrmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HrmsApplication.class, args);
    }
}
