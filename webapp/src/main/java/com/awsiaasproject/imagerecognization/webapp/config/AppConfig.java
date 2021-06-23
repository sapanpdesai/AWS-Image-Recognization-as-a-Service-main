package com.awsiaasproject.imagerecognization.webapp.config;

import com.awsiaasproject.imagerecognization.webapp.repo.AWSS3Repo;
import com.awsiaasproject.imagerecognization.webapp.repo.AWSS3RepoImpl;
import com.awsiaasproject.imagerecognization.webapp.service.AWSUtil;
import com.awsiaasproject.imagerecognization.webapp.service.AWSUtilImpl;
import com.awsiaasproject.imagerecognization.webapp.service.BusinessLogic;
import com.awsiaasproject.imagerecognization.webapp.service.BusinessLogicImpl;
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
    public BusinessLogic runImageClassification() {
        return new BusinessLogicImpl();
    }

    @Bean
    public AWSS3Repo getS3Client() {
        return new AWSS3RepoImpl();
    }
}
