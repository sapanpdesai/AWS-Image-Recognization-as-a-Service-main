package com.awsiaasproject.imagerecognization.webapp.service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.sqs.model.Message;
import com.awsiaasproject.imagerecognization.webapp.constant.ProjectConstant;

/*
 * This class contain logic to provide image classification result to user.
 */
@Service
public class BusinessLogicImpl implements BusinessLogic, Runnable {
	
	@Autowired
	private AWSUtil awsService;

	@Override
	public void run() {
		this.putOutputFromResponseQueueToHashMap();
	}
	
	/*
	 * This method keeps on running in background and put all the messages from response queue to hashmap.
	 */
	@Override
	public void putOutputFromResponseQueueToHashMap() {
		while (true) {
			List<Message> msgList = null;
			try {
				msgList = awsService.receiveMessage(ProjectConstant.OUTPUT_QUEUE, 20, ProjectConstant.MAX_WAIT_TIME_OUT, 10);
				if (msgList != null) {
					try {
						for (Message msg : msgList) {
							String[] classificationResult = null;
							classificationResult = msg.getBody().split(ProjectConstant.INPUT_OUTPUT_SEPARATOR);
							outputMap.put(classificationResult[0], classificationResult[1]);
							awsService.deleteMessage(msg, ProjectConstant.OUTPUT_QUEUE);
						}
					} catch (Exception w) {
						System.out.println("Error in putting file from queue to map");
					}
				}
			} catch (Exception e) {
				System.out.println("No Msg Available: " + e.getMessage());
				System.out.println("Thread sleeping 10sec");
				try {
					Thread.sleep(10000);
				} catch (Exception p) {
					System.out.println("Thread not sleeping some error");
				}
			}
		}
	}
	
	@Override
	public String[] getImageClassificationResult(String imageURL) {
		String[] output = awsService.getOutputFromResponseQueue(imageURL);
		return output;
	}

	/*
	 * Convert file type from Spring web multipart to java.io.file
	 */
	public File convertMultiPartToFile(MultipartFile file) {
		try {
			File convFile = new File(file.getOriginalFilename());
			FileOutputStream fos = new FileOutputStream(convFile);
			fos.write(file.getBytes());
			fos.close();
			return convFile;
		} catch (Exception e) {
			return null;
		}

	}
	
	/*
	 * This method generate unique filename to distinguised request from differnt users.
	 */
	@Override
	public String generateFileName(MultipartFile multiPart) {
		return System.currentTimeMillis() + "-" + multiPart.getOriginalFilename().replace(" ", "_");
	}
	
	/*
	 * This method generate unique filename to distinguised request from differnt users.
	 */
	@Override
	public String formatImageUrl(String imageUrl) {
		int firstIndex = imageUrl.indexOf('-');
		int lastIndex = imageUrl.lastIndexOf('.');
		return imageUrl.substring(firstIndex + 1, lastIndex);
	}

}
