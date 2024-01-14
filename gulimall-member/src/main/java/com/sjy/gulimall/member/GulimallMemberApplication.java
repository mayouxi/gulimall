package com.sjy.gulimall.member;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@MapperScan("com.sjy.gulimall.member.dao")
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.sjy.gulimall.member.feign")
public class GulimallMemberApplication {
    public static void main(String[] args) {
        SpringApplication.run(GulimallMemberApplication.class, args);
    }
}