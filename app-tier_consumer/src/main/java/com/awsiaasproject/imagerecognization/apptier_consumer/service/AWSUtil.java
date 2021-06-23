package com.awsiaasproject.imagerecognization.apptier_consumer.service;

import com.amazonaws.services.sqs.model.Message;

/*
 * All the aws services requires to run the project are defined here.
 */
public interface AWSUtil {
	public void queueResponse(String url, String queueName, int delay);

	public void createQueue(String queueName);

	public void scaleIn();

	public void deleteMessage(Message message, String queueName);

	public Message receiveMessage(String queueName, Integer visibilityTimeout, Integer waitTimeOut);

	public void terminateInstance();
	
	public Integer getTotalNumberOfMessagesInQueue(String queueName);
}
