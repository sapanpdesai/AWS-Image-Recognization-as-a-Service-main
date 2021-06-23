package com.awsiaasproject.imagerecognization.apptier_consumer.service;

import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import com.awsiaasproject.imagerecognization.apptier_consumer.config.BasicAWSConfigurations;
import com.awsiaasproject.imagerecognization.apptier_consumer.constant.ProjectConstant;
import com.awsiaasproject.imagerecognization.apptier_consumer.repo.AWSS3Repo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Arrays;

@Service
public class AWSUtilImpl implements AWSUtil, Runnable {

	@Autowired
	private BasicAWSConfigurations awsConfigurations;

	@Autowired
	private AWSS3Repo s3Service;

	@Override
	public void run() {
		this.scaleIn();
	}
	
	/*
	 * @param url - url of the image as in image name
	 * @param queueName : Queue in which you want to save the messages.
	 * @param delay: Time until which msg will not be able to processed in Amazon SQS.
	 */
	@Override
	public void queueResponse(String message, String queueName, int delay) {
		String queueUrl = null;

		try {
			queueUrl = awsConfigurations.getSQSService().getQueueUrl(queueName).getQueueUrl();
		} catch (Exception e) {
			createQueue(queueName);
		}
		queueUrl = awsConfigurations.getSQSService().getQueueUrl(queueName).getQueueUrl();
		awsConfigurations.getSQSService().sendMessage(new SendMessageRequest().withQueueUrl(queueUrl)
				.withMessageGroupId(UUID.randomUUID().toString()).withMessageBody(message).withDelaySeconds(0));
	}
	
	/*
	 * @param queueName : Name of the queue name.
	 * This method generate new queue using Amazon SQS.
	 */
	@Override
	public void createQueue(String queueName) {
		CreateQueueRequest createQueueRequest = new CreateQueueRequest().withQueueName(queueName)
				.addAttributesEntry(QueueAttributeName.FifoQueue.toString(), Boolean.TRUE.toString())
				.addAttributesEntry(QueueAttributeName.ContentBasedDeduplication.toString(), Boolean.TRUE.toString());
		awsConfigurations.getSQSService().createQueue(createQueueRequest);
	}
	
	/*
	 * This method will scale in, terminate app instance when demand is low. 
	 */
	@Override
	public void scaleIn() {
		while (true) {

			Message msg = receiveMessage(ProjectConstant.INPUT_QUEUE, ProjectConstant.MAX_VISIBILITY_TIMEOUT,
					ProjectConstant.MAX_WAIT_TIME_OUT);
			if (msg != null) {
				try {
					S3Object object = awsConfigurations.getS3()
							.getObject(new GetObjectRequest(ProjectConstant.INPUT_BUCKET, msg.getBody()));
					InputStream is = object.getObjectContent();

					File imageFile = new File(ProjectConstant.PATH_TO_DIRECTORY + msg.getBody());
					FileOutputStream fos = new FileOutputStream(imageFile);
					byte[] buf = new byte[8192];
					int length;
					while ((length = is.read(buf)) > 0) {
						fos.write(buf, 0, length);
					}
					fos.close();
					String predicted_value = runPythonScript(msg.getBody());
					this.queueResponse(msg.getBody() + ProjectConstant.INPUT_OUTPUT_SEPARATOR + predicted_value,
							ProjectConstant.OUTPUT_QUEUE, 0);
					s3Service.uploadFile(formatInputRequest(msg.getBody()), predicted_value);
					imageFile.delete();
					deleteMessage(msg, ProjectConstant.INPUT_QUEUE);
				} catch (Exception w) {
					w.printStackTrace();
				}
			} else {
				break;
			}
		}
	}

	/*
	 * Delete messges from the given queue.
	 */
	@Override
	public void deleteMessage(Message message, String queueName) {
		String queueUrl = awsConfigurations.getSQSService().getQueueUrl(queueName).getQueueUrl();
		String messageReceiptHandle = message.getReceiptHandle();
		DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(queueUrl, messageReceiptHandle);
		awsConfigurations.getSQSService().deleteMessage(deleteMessageRequest);
	}
	
	/*
	 * @param visibilityTimeout : Time up to which msg will not be available to cousume by another consumer.
	 * @param waitTimeOut : Buffer Time until which queue will wait to receive message. 
	 * @Return : List of messgae present in queue.
	 * The idea to put this service in sleep is to reduce number of call to AWS SQS as this is polling to service.
	 */
	@Override
	public Message receiveMessage(String queueName, Integer visibilityTimeout, Integer waitTimeOut) {
		String queueUrl = awsConfigurations.getSQSService().getQueueUrl(queueName).getQueueUrl();
		ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
		receiveMessageRequest.setMaxNumberOfMessages(1);
		receiveMessageRequest.setVisibilityTimeout(visibilityTimeout);
		receiveMessageRequest.setWaitTimeSeconds(waitTimeOut);
		ReceiveMessageResult receiveMessageResult = awsConfigurations.getSQSService()
				.receiveMessage(receiveMessageRequest);
		List<Message> messageList = receiveMessageResult.getMessages();
		if (messageList.isEmpty()) {
			return null;
		}
		return messageList.get(0);
	}
	
	/*
	 * Terminate app-tier instance
	 */
	@Override
	public void terminateInstance() {
		String myId = EC2MetadataUtils.getInstanceId();
		TerminateInstancesRequest request = new TerminateInstancesRequest().withInstanceIds(myId);
		awsConfigurations.getEC2Service().terminateInstances(request);
	}
	
	
	/*
	 * @return Total number of message present in particular queue.
	 */
	@Override
	public Integer getTotalNumberOfMessagesInQueue(String queueName) {
		String queueUrl = null;

		try {
			queueUrl = awsConfigurations.getSQSService().getQueueUrl(queueName).getQueueUrl();
		} catch (Exception e) {
			createQueue(queueName);
		}
		queueUrl = awsConfigurations.getSQSService().getQueueUrl(queueName).getQueueUrl();
		GetQueueAttributesRequest getQueueAttributesRequest = new GetQueueAttributesRequest(queueUrl,
				ProjectConstant.SQS_METRICS);
		Map<String, String> map = awsConfigurations.getSQSService().getQueueAttributes(getQueueAttributesRequest)
				.getAttributes();

		return Integer.parseInt((String) map.get(ProjectConstant.TOTAL_MSG_IN_SQS));
	}
	
	/*
	 * Run python script present on app instance to classify image using process builder.
	 * Remember, process is heavier than thread -> you can improve this using differnt libraries to carry out 
	 * cross out 
	 */
	public String runPythonScript(String fileName) {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder("python3",
					resolvePythonScriptPath(ProjectConstant.PYTHON_SCRIPT), resolvePythonScriptPath(fileName));
			processBuilder.redirectErrorStream(true);

			Process process = processBuilder.start();
			List<String> results = readProcessOutput(process.getInputStream());

			if (!results.isEmpty() && results != null)
				return results.get(0);
			return "Sorry!!! Algo not able to process your request";
		} catch (Exception e) {
			return "Sorry!!! Algo not able to process your request";
		}
	}

	private static List<String> readProcessOutput(InputStream inputStream) {
		try {
			BufferedReader output = new BufferedReader(new InputStreamReader(inputStream));
			return output.lines().collect(Collectors.toList());
		} catch (Exception e) {
			return new ArrayList<>(Arrays.asList("No Prediction"));
		}
	}

	private static String resolvePythonScriptPath(String filename) {
		File file = new File(ProjectConstant.PATH_TO_DIRECTORY + filename);
		return file.getAbsolutePath();
	}

	public String formatInputRequest(String fileName) {
		int firstIndex = fileName.indexOf("-");
		int lastIndex = fileName.lastIndexOf(".");
		return fileName.substring(firstIndex + 1, lastIndex);
	}
}
