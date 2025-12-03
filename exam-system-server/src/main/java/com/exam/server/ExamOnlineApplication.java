package com.exam.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@MapperScan({
    "com.exam.userService.dal.mysqlmapper",
    "com.exam.manage.dal.mysqlmapper",
    "com.exam.excute.dal.mysqlmapper",
    "com.exam.analyse.dal.mysqlmapper",
    "com.exam.highConcurrencyDisposal.dal.mysqlmapper"
})
@SpringBootApplication
@ComponentScan(basePackages = {"com.exam"})
public class ExamOnlineApplication {
    public static void main(String[] args) {
        SpringApplication.run(ExamOnlineApplication.class, args);
    }
}
