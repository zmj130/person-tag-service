package com.qianfan.tag;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 人员标签服务启动类。 */
@MapperScan("com.qianfan.tag.mapper")
@SpringBootApplication
public class PersonTagApplication {
    public static void main(String[] args) {
        SpringApplication.run(PersonTagApplication.class, args);
    }
}

