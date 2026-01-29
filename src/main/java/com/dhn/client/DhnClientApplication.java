package com.dhn.client;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableRetry
@EnableAspectJAutoProxy(exposeProxy = true)
public class DhnClientApplication{

	public static void main(String[] args) {
		SpringApplication.run(DhnClientApplication.class, args);
	}


}
