package com.documentreaderai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DocumentreaderaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentreaderaiApplication.class, args);
	}

}
