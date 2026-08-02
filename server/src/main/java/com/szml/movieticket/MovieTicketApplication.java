package com.szml.movieticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 电影票智能体启动类。
 *
 * @author zhanghao
 * @since 2026-07-30
 */
@SpringBootApplication
@EnableScheduling
public class MovieTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(MovieTicketApplication.class, args);
    }
}
