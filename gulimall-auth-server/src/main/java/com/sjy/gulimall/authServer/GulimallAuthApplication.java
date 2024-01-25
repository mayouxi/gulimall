package com.sjy.gulimall.authServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
//可以远程调用,使服务能够被nacos发现
@EnableFeignClients
@EnableDiscoveryClient
public class GulimallAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(GulimallAuthApplication.class, args);
    }
}
