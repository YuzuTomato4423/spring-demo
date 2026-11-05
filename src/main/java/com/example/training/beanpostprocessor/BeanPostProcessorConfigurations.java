package com.example.training.beanpostprocessor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import com.example.demo.HelloController;
import com.example.demo.config.AppConfig;
import com.example.demo.config.EmployeeNotificationConfig;
import com.example.demo.notification.EmployeeRegistrationNotifier;
import com.example.demo.controller.EmployeeController;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.repository.InMemoryEmployeeRepository;
import com.example.demo.service.EmployeeService;

/** 既存のBeanを実習専用Contextへ登録。通常アプリのComponent Scan範囲外に置く。 */
public final class BeanPostProcessorConfigurations {
    private BeanPostProcessorConfigurations() { }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackages = "com.example.demo", useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = {EmployeeController.class, EmployeeService.class, EmployeeMapper.class,
                            InMemoryEmployeeRepository.class, AppConfig.class, HelloController.class,
                            EmployeeNotificationConfig.class, EmployeeRegistrationNotifier.class}))
    // Bootを使わない既存実習Contextにも、HelloControllerの共通設定を供給する。
    @PropertySource("classpath:application.properties")
    static class ApplicationBeans { }

    @Configuration(proxyBeanMethods = false)
    @Import(ApplicationBeans.class)
    static class AllBeans {
        @Bean
        static EmployeeBeanPostProcessor employeeBeanPostProcessor() {
            // staticなfactoryで早期生成し、設定クラスや業務Beanの不要な早期初期化を避ける。
            return new EmployeeBeanPostProcessor(false);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ApplicationBeans.class)
    static class EmployeeOnly {
        @Bean
        static EmployeeBeanPostProcessor employeeBeanPostProcessor() {
            return new EmployeeBeanPostProcessor(true);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(EmployeeOnly.class)
    static class Lifecycle {
        @Bean
        EmployeeLifecycleProbe employeeLifecycleProbe(EmployeeService service,
                EmployeeBeanPostProcessor observer) {
            return new EmployeeLifecycleProbe(service, observer);
        }
    }
}
