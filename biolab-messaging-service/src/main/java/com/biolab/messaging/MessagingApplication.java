package com.biolab.messaging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.biolab.messaging",
        "com.biolab.common.encryption",
        "com.biolab.common.security",
        "com.biolab.common.logging",
        "com.biolab.common.rls",
        "com.biolab.common.audit"
})
public class MessagingApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessagingApplication.class, args);
    }
}
