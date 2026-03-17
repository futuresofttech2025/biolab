package com.biolab.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.TimeZone;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.biolab.audit",
        "com.biolab.common.encryption",
        "com.biolab.common.security",
        "com.biolab.common.logging",
        "com.biolab.common.rls",
        "com.biolab.common.audit"
})
public class AuditApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(AuditApplication.class, args);
    }
}
