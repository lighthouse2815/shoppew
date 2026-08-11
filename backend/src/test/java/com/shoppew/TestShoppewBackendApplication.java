package com.shoppew;

import org.springframework.boot.SpringApplication;

public class TestShoppewBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(ShoppewBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
