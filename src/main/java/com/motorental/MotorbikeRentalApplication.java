package com.motorental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MotorbikeRentalApplication {

	public static void main(String[] args) {
		SpringApplication.run(MotorbikeRentalApplication.class, args);
	}

}
