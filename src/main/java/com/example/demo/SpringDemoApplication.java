package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Component Scanはこのクラスのcom.example.demoとその配下を対象にする。
 * Repository・Service・Controller・MapperをBeanとしてApplicationContextへ登録し、
 * 生成と依存注入をSpringに任せる。com.example.outsideは既定の走査範囲外。
 */
@SpringBootApplication
public class SpringDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringDemoApplication.class, args);
    }
}
