package com.awsiaasproject.imagerecognization.webapp.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.InstanceStateName;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.util.Base64;
import com.awsiaasproject.imagerecognization.webapp.config.BasicAWSConfigurations;
import com.awsiaasproject.imagerecognization.webapp.constant.ProjectConstant;
import com.awsiaasproject.imagerecognization.webapp.repo.AWSS3Repo;

/*
 * This class implements all the method of AWSUtil and define specific 
 * behaviour of each method requires to run the application.
 */
@Service
public class AWSUtilImpl implements AWSUtil, Runnable {

	@Autowired
	private BasicAWSConfigurations awsConfigurations;

	@Autowired
	private BusinessLogic backendService;

	@Autowired
	private AWSS3Repo s3Repo;

	@Override
	public void run() {
		System.out.println("Thread 2 Started");
		this.scaleOut();
	}
	
	/*
	 * @param imageId : Amazon machine image - To start instance of that particular image
	 * @param instanceType : EC2 instance type - More detail - https://aws.amazon.com/ec2/instance-types/
	 * @param minInstance : Minimum Number of Instances - To make sure that Amazon EC2 start minimum number of instances
	 * @param maxInstance : Maximum Number of Instances - Amazon EC2 can start upto maximum number of instances.
	 * @return : None
	 */
	@Override
	public void createAndRunInstance(String imageId, String instanceType, Integer minInstance, Integer maxInstance) {
		try {
			Integer totalNumberOfAppInstancesRunning = getTotalNumOfInstances();
			if (totalNumberOfAppInstancesRunning + maxInstance > ProjectConstant.MAX_NUM_OF_APP_INSTANCES) {
				if (ProjectConstant.MAX_NUM_OF_APP_INSTANCES - totalNumberOfAppInstancesRunning > 0) {
					maxInstance = ProjectConstant.MAX_NUM_OF_APP_INSTANCES - totalNumberOfAppInstancesRunning;
					if (maxInstance == 1)
						minInstance = 1;
					else
						minInstance = maxInstance - 1;
				} else {
					return;
				}
			}

			
			Collection<Tag> tagsForAppInstance = new ArrayList<>();
			TagSpecification ts = new TagSpecification();
			Tag tag = new Tag();
			tag.setKey(ProjectConstant.TAG_KEY);
			tag.setValue(ProjectConstant.TAG_VALUE);
			tagsForAppInstance.add(tag);
			ts.setResourceType(ProjectConstant.RESOURCE_INSTANCE);
			ts.setTags(tagsForAppInstance);

			RunInstancesRequest runInstancesRequest = new RunInstancesRequest().withImageId(imageId)
					.withInstanceType(instanceType).withMinCount(minInstance).withMaxCount(maxInstance)
					.withKeyName(ProjectConstant.PRIVATE_KEY).withTagSpecifications(ts)
					.withUserData(new String(Base64.encode(ProjectConstant.USER_DATA.getBytes("UTF-8")), "UTF-8"));

			awsConfigurations.getEC2Service().runInstances(runInstancesRequest);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	/*
	 * @param imageId : Amazon machine image - To start instance of that particular image
	 * @param instanceType : EC2 instance type - More detail - https://aws.amazon.com/ec2/instance-types/
	 * @param minInstance : Minimum Number of Instances - To make sure that Amazon EC2 start minimum number of instances
	 * @param maxInstance : Maximum Number of Instances - Amazon EC2 can start upto maximum number of instances.
	 * @return : None
	 */
	@Override
	public Integer getTotalNumOfInstances() {
		DescribeInstanceStatusRequest describeInstanceStatusRequest = new DescribeInstanceStatusRequest();
		describeInstanceStatusRequest.setIncludeAllInstances(true);
		DescribeInstanceStatusResult describeInstances = awsConfigurations.getEC2Service()
				.describeInstanceStatus(describeInstanceStatusRequest);
		List<InstanceStatus> instanceStatusList = describeInstances.getInstanceStatuses();
		Integer total = 0;
		for (InstanceStatus is : instanceStatusList)
			if (is.getInstanceState().getName().equals(InstanceStateName.Running.toString())
					|| is.getInstanceState().getName().equals(InstanceStateName.Pending.toString()))
				total++;

		return total - 1;
	}
	
	/*
	 * @param url - url of the image as in image name
	 * @param queueName : Queue in which you want to save the messages.
	 * @param delay: Time until which msg will not be able to processed in Amazon SQS.
	 */
	@Override
	public void queueInputRequest(String url, String queueName, int delay) {
		String queueUrl = null;

		try {
			queueUrl = awsConfigurations.getSQSService().getQueueUrl(queueName).getQueueUrl();
		} catch (Exception e) {
			createQueue(queueName);
		}
		queueUrl = awsConfigurations.getSQSService().getQueueUrl(queueName).getQueueUrl();
		awsConfigurations.getSQSService().sendMessage(new SendMessageRequest().withQueueUrl(queueUrl)
				.withMessageGroupId(UUID.randomUUID().toString()).withMessageBody(url).withDelaySeconds(0));
	}

	/*
	 * @param imageUrl : Put the output from response queue to hashmap.
	 * This function put the result of image recognization from response queue to hashmap.
	 * Idea to use hashMap was to fasten the process in order to reduce runtime.
	 */
	@Override
	public String[] getOutputFromResponseQueue(String imageUrl) {
		System.out.println("ImageURL: " + imageUrl);
		while (true) {
			try {
				if (BusinessLogic.outputMap.containsKey(imageUrl)) {
					String output = BusinessLogic.outputMap.get(imageUrl);
					BusinessLogic.outputMap.remove(imageUrl);
					return new String[] { backendService.formatImageUrl(imageUrl), output };
				} else {
					try {
						Thread.sleep(8000);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				System.out.println("Some Error while getting outPut from HashMap");
				try {
					Thread.sleep(8000);
				} catch (Exception o) {
					o.printStackTrace();
				}
			}
		}
	}
	/*
	 * @param queueName : Name of the queue name.
	 * This method generate new queue using Amazon SQS.
	 */
	@Override
	public void createQueue(String queueName) {
		try {
			CreateQueueRequest createQueueRequest = new CreateQueueRequest().withQueueName(queueName)
					.addAttributesEntry(QueueAttributeName.FifoQueue.toString(), Boolean.TRUE.toString())
					.addAttributesEntry(QueueAttributeName.ContentBasedDeduplication.toString(),
							Boolean.TRUE.toString());

			awsConfigurations.getSQSService().createQueue(createQueueRequest);

		} catch (Exception e) {
			System.out.println("Error while creating queue: " + e.getMessage());
		}
	}
	
	/*
	 * @param multipart : class of spring java to get multiple image input from user.
	 * @param fileName : Name of file which will used as key.
	 */
	@Override
	public void uploadFileToS3(MultipartFile multipartFile, String fileName) {
		try {
			File file = backendService.convertMultiPartToFile(multipartFile);
			s3Repo.uploadFile(fileName, file);
			file.delete();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * This method will scale up, generate new app instances when demand is high. 
	 * Limitation is, it will only generate maximum of 19 app instances due to Amazon free tier.
	 * Nothing comes free :). 
	 */
	@Override
	public void scaleOut() {
		while (true) {
			Integer totalNumberOfMsgInQueue = getTotalNumberOfMessagesInQueue(ProjectConstant.INPUT_QUEUE);
			Integer totalNumberOfAppInstancesRunning = getTotalNumOfInstances();
			Integer numberOfInstancesToRun = 0;
			if (totalNumberOfAppInstancesRunning < totalNumberOfMsgInQueue) {
				if (totalNumberOfMsgInQueue
						- totalNumberOfAppInstancesRunning < ProjectConstant.MAX_NUM_OF_APP_INSTANCES) {
					numberOfInstancesToRun = totalNumberOfMsgInQueue - totalNumberOfAppInstancesRunning;
				} else {
					numberOfInstancesToRun = ProjectConstant.MAX_NUM_OF_APP_INSTANCES
							- totalNumberOfAppInstancesRunning;
				}
			}
			System.out.println("numberOfInstancesToRun: " + numberOfInstancesToRun);

			if (numberOfInstancesToRun == 1) {
				createAndRunInstance(ProjectConstant.AMI_ID, ProjectConstant.INSTANCE_TYPE, 1, 1);
			} else if (numberOfInstancesToRun > 1) {
				createAndRunInstance(ProjectConstant.AMI_ID, ProjectConstant.INSTANCE_TYPE, numberOfInstancesToRun - 1,
						numberOfInstancesToRun);
			}
			try {
				Thread.sleep(2000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
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
	 * @param visibilityTimeout : Time up to which msg will not be available to cousume by another consumer.
	 * @param waitTimeOut : Buffer Time until which queue will wait to receive message. 
	 * @Return : List of messgae present in queue.
	 * The idea to put this service in sleep is to reduce number of call to AWS SQS as this is polling to service.
	 */
	@Override
	public List<Message> receiveMessage(String queueName, Integer visibilityTimeout, Integer waitTimeOut,
			Integer maxNumOfMsg) {
		try {
			String queueUrl = awsConfigurations.getSQSService().getQueueUrl(queueName).getQueueUrl();
			ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
			receiveMessageRequest.setMaxNumberOfMessages(maxNumOfMsg);
			receiveMessageRequest.setVisibilityTimeout(visibilityTimeout);
			receiveMessageRequest.setWaitTimeSeconds(waitTimeOut);
			ReceiveMessageResult receiveMessageResult = awsConfigurations.getSQSService()
					.receiveMessage(receiveMessageRequest);
			List<Message> messageList = receiveMessageResult.getMessages();
			if (messageList.isEmpty()) {
				System.out.println("Msg List empty");
				return null;
			}
			return messageList;
		} catch (Exception e) {
			System.out.println("No Msg Available: " + e.getMessage());
			System.out.println("Thread sleeping 10sec");
			try {
				Thread.sleep(10000);
			} catch (Exception p) {
				System.out.println("Thread not sleeping some error");
			}
			return null;
		}
	}
	
	/*
	 * Delete messges from the given queue.
	 */
	@Override
	public void deleteMessage(Message message, String queueName) {
		try {
			String queueUrl = awsConfigurations.getSQSService().getQueueUrl(queueName).getQueueUrl();
			String messageReceiptHandle = message.getReceiptHandle();
			DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(queueUrl, messageReceiptHandle);
			awsConfigurations.getSQSService().deleteMessage(deleteMessageRequest);
		} catch (Exception e) {
			System.out.println("Error while deleting msg from: " + e.getMessage());
		}
	}
	
	/*
	 * It will configured basic AWS Services such as creating new Queues and S3 buckets.
	 * Do not call this method again and again as S3 bucket are public you will encounter an error if someone has created
	 * bucket with same name as yours. Make sure to setup project and never used this service again and again.
	 * It is just for testing.
	 */
	@Override
	public void configuredBasicAWSServices(String inputQueue, String outputQueue, String inputBucket,
			String outputBucket) {
		try {
			try {
				awsConfigurations.getSQSService().getQueueUrl(inputQueue).getQueueUrl();
			} catch (Exception e) {
				createQueue(inputQueue);
			}

			try {
				awsConfigurations.getSQSService().getQueueUrl(outputQueue).getQueueUrl();
			} catch (Exception e) {
				createQueue(outputQueue);
			}

			try {
				if (!awsConfigurations.getS3().doesBucketExistV2(inputBucket))
					awsConfigurations.getS3().createBucket(inputBucket);
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				if (!awsConfigurations.getS3().doesBucketExistV2(outputBucket))
					awsConfigurations.getS3().createBucket(outputBucket);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * It will configured basic AWS Services such as Deleting Queues and S3 buckets if any such present in your account.
	 * Do not call this method again and again as S3 bucket are public you will encounter an error if someone has created
	 * bucket with same name as yours. Make sure to setup project and never used this service again and again.
	 * It is just for testing.
	 */
	@Override
	public void resetBasicAWSServices(String inputQueue, String outputQueue, String inputBucket, String outputBucket) {
		try {
			try {
				String inputQueueUrl = awsConfigurations.getSQSService().getQueueUrl(inputQueue).getQueueUrl();
				awsConfigurations.getSQSService().deleteQueue(new DeleteQueueRequest(inputQueueUrl));
			} catch (Exception e) {
				System.out.println("Error deleting inputQueue: " + e.getMessage());
			}

			try {
				String outputQueueUrl = awsConfigurations.getSQSService().getQueueUrl(outputQueue).getQueueUrl();
				awsConfigurations.getSQSService().deleteQueue(new DeleteQueueRequest(outputQueueUrl));
			} catch (Exception e) {
				System.out.println("Error deleting outputQueue: " + e.getMessage());
			}

			try {
				this.deleteBucket(inputBucket);
			} catch (Exception e) {
				System.out.println("Error deleting inputBucket: " + e.getMessage());
			}

			try {
				this.deleteBucket(outputBucket);
			} catch (Exception e) {
				System.out.println("Error deleting outputBucket: " + e.getMessage());
			}
		} catch (Exception e) {
			System.out.println("Error in resetting in one of the function: " + e.getMessage());
		}
	}
	
	/*
	 * Delete bucket from AWS S3.
	 */
	@Override
	public void deleteBucket(String bucketName) {
		try {
			ObjectListing objectListing = awsConfigurations.getS3().listObjects(bucketName);
			while (true) {
				Iterator<S3ObjectSummary> objIter = objectListing.getObjectSummaries().iterator();
				while (objIter.hasNext()) {
					awsConfigurations.getS3().deleteObject(bucketName, objIter.next().getKey());
				}

				if (objectListing.isTruncated()) {
					objectListing = awsConfigurations.getS3().listNextBatchOfObjects(objectListing);
				} else {
					break;
				}
			}
			VersionListing versionList = awsConfigurations.getS3()
					.listVersions(new ListVersionsRequest().withBucketName(bucketName));
			while (true) {
				Iterator<S3VersionSummary> versionIter = versionList.getVersionSummaries().iterator();
				while (versionIter.hasNext()) {
					S3VersionSummary vs = versionIter.next();
					awsConfigurations.getS3().deleteVersion(bucketName, vs.getKey(), vs.getVersionId());
				}

				if (versionList.isTruncated()) {
					versionList = awsConfigurations.getS3().listNextBatchOfVersions(versionList);
				} else {
					break;
				}
			}
			awsConfigurations.getS3().deleteBucket(bucketName);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
