package com.hj.blogBatch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// @EnableBatchProcessing
public class BlogBatchApplication {

	public static void main(String[] args) {
		// SpringApplication.run(BlogBatchApplication.class, args);
		System.exit(SpringApplication.exit(SpringApplication.run(BlogBatchApplication.class, args)));
	}

}
