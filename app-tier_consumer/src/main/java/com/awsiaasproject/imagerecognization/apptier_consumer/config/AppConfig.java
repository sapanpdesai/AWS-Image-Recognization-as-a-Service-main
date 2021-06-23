package com.awsiaasproject.imagerecognization.apptier_consumer.config;

import com.awsiaasproject.imagerecognization.apptier_consumer.repo.AWSS3Repo;
import com.awsiaasproject.imagerecognization.apptier_consumer.repo.AWSS3RepoImpl;
import com.awsiaasproject.imagerecognization.apptier_consumer.service.AWSUtil;
import com.awsiaasproject.imagerecognization.apptier_consumer.service.AWSUtilImpl;
import org.springframework.context.annotation.Bean;

/*
 * Configuration class that produce beans necessary for web tier to run. 
 * All these beans are handled by IoC controller and configured them when the application start makes the application
 * loosely coupled as it inject dependency automatically. 
 * As Spring beans can be reusable these object are created only once during start of the application where our 
 * Spring IoC is initialized and configured all the beans. 
 */
public class AppConfig {

	@Bean
	public AWSUtil getAWSService() {
		return new AWSUtilImpl();
	}

	@Bean
	public BasicAWSConfigurations awsConfigurations() {
		return new BasicAWSConfigurations();
	}

	@Bean
	public AWSS3Repo getS3Client() {
		return new AWSS3RepoImpl();
	}
}
