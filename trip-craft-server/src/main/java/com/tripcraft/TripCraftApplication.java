package com.tripcraft;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * TripCraft (伴游行) 后端服务启动类.
 */
@SpringBootApplication
@MapperScan("com.tripcraft.mapper")
public class TripCraftApplication {

    public static void main(String[] args) {
        SpringApplication.run(TripCraftApplication.class, args);
        System.out.println("***************************TripCraft (伴游行) 后端服务启动成功***************************");
    }
}
