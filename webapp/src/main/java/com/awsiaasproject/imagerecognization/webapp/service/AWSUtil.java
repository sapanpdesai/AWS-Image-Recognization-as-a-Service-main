package com.awsiaasproject.imagerecognization.webapp.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import com.amazonaws.services.sqs.model.Message;

/*
 * All the aws services requires to run the project are defined here.
 */
public interface AWSUtil {

	public void createAndRunInstance(String imageId, String instanceType, Integer minInstance, Integer maxInstance);

	public void queueInputRequest(String url, String queueName, int delay);
	
	public List<Message> receiveMessage(String queueName, Integer visibilityTimeout, Integer waitTimeOut, Integer maxNumOfMsg);

	public String[] getOutputFromResponseQueue(String imageURL);

	public void createQueue(String queueName);

	public void uploadFileToS3(MultipartFile multipartFile, String fileName);

	public void scaleOut();

	public Integer getTotalNumberOfMessagesInQueue(String queueName);

	public Integer getTotalNumOfInstances();

	public void deleteMessage(Message message, String queueName);

	public void configuredBasicAWSServices(String inputQueue, String outputQueue, String inputBucket,
			String outputBucket);
	
	public void resetBasicAWSServices(String inputQueue, String outputQueue, String inputBucket, String outputBucket);
	
	public void deleteBucket(String bucketName);
}
