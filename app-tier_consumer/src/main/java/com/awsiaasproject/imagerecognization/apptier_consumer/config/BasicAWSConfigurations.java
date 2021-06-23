package com.awsiaasproject.imagerecognization.apptier_consumer.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.awsiaasproject.imagerecognization.apptier_consumer.constant.ProjectConstant;
import org.springframework.context.annotation.Configuration;

/*
 * @Configuration supply bean metadata to Spring IoC controller 
 */
@Configuration
public class BasicAWSConfigurations {

	public BasicAWSCredentials basicAWSCredentials() {
		return new BasicAWSCredentials(ProjectConstant.ACCESS_ID, ProjectConstant.ACCESS_KEY);
	}

	public AmazonEC2 getEC2Service() {
		return AmazonEC2ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials()))
				.withRegion(ProjectConstant.AWS_REGION).build();
	}

	public AmazonSQS getSQSService() {
		return AmazonSQSClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials()))
				.withRegion(ProjectConstant.AWS_REGION).build();
	}

	public AmazonS3 getS3() {
		return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials()))
				.withRegion(ProjectConstant.AWS_REGION).build();
	}

}
