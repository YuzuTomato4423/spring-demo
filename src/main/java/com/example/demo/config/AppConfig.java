package com.example.demo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.EmployeeFormatter;
import com.example.demo.FormatterHolder;
import com.example.demo.SimpleEmployeeFormatter;

/** 表示と通知で同じ型付き設定を共有し、設定読込を利用側へ重複させない。 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    @Bean
    public SimpleEmployeeFormatter employeeFormatter() {
        return new SimpleEmployeeFormatter();
    }

    @Bean
    public FormatterHolder formatterHolder(EmployeeFormatter employeeFormatter) {
        return new FormatterHolder(employeeFormatter);
    }
}
