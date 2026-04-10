package com.wchuan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.wchuan.system.mapper")
@EnableAsync
@EnableScheduling
public class WchuanApplication {

    public static void main(String[] args) {
        SpringApplication.run(WchuanApplication.class, args);
    }

}
