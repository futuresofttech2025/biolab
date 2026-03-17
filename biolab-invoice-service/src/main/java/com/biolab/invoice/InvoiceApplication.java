package com.biolab.invoice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.TimeZone;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.biolab.invoice",
        "com.biolab.common.encryption",
        "com.biolab.common.security",
        "com.biolab.common.logging",
        "com.biolab.common.rls",
        "com.biolab.common.audit"
})
public class InvoiceApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(InvoiceApplication.class, args);
    }
}
