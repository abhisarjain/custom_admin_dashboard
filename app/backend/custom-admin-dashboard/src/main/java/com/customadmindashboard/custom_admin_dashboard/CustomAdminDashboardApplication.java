package com.customadmindashboard.custom_admin_dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.customadmindashboard")
@EnableJpaRepositories(basePackages = "com.customadmindashboard")
@EntityScan(basePackages = "com.customadmindashboard")
public class CustomAdminDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(CustomAdminDashboardApplication.class, args);
    }
}