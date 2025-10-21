package com.soluvis.ds.apigw.v1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;

/**
 * @Class 		: Application
 * @date   		: 2025. 4. 8.
 * @author   	: PA2412013
 * ----------------------------------------
 * @notify
 *  Springboot Application
 * 
 *  1. Feign 허용
 *  2. PropertiesScan 허용
 */
@EnableFeignClients
@ConfigurationPropertiesScan
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
//		ResourceLeakDetector.setLevel(Level.PARANOID);
		SpringApplication.run(Application.class, args);
	}

}
