package com.shoppew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShoppewBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShoppewBackendApplication.class, args);
	}

}
