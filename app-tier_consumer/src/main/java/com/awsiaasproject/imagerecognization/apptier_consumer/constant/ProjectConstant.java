package com.awsiaasproject.imagerecognization.apptier_consumer.constant;

import com.amazonaws.regions.Regions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * All the necessary constants require for the project are defined here so they can be reuse.
 */
public class ProjectConstant {

	public static final String ACCESS_ID = "";

	public static final String ACCESS_KEY = "";

	public static final Regions AWS_REGION = Regions.US_EAST_1;

	public static final String AMI_ID = "";

	public static final String INSTANCE_TYPE = "t2.micro";

	public static final String PRIVATE_KEY = "";

	public static final String SECURITY_GROUP = "security";

	public static final String INPUT_QUEUE = "inputQueue.fifo";

	public static final String OUTPUT_QUEUE = "outputQueue.fifo";

	public static final String INPUT_BUCKET = "image-input-bucket-cc-version-1";

	public static final String OUTPUT_BUCKET = "image-output-bucket-cc-version-1";

	public static final String TOTAL_MSG_IN_SQS = "ApproximateNumberOfMessages";

	public static final Integer MAX_NUM_OF_APP_INSTANCES = 19;

	public static final List<String> SQS_METRICS = new ArrayList<String>(Arrays.asList(TOTAL_MSG_IN_SQS));

	public static final List<String> SECURITY_GROUP_LIST = new ArrayList<>(Arrays.asList(SECURITY_GROUP));

	public static final String TAG_KEY = "Name";

	public static final String TAG_VALUE = "App Instance";

	public static final String RESOURCE_INSTANCE = "instance";

	public static final String PATH_TO_DIRECTORY = "/home/ubuntu/classifier/";

	public static final String PYTHON_SCRIPT = "image_classification.py";

	public static final String MESSAGE_GROUP_ID = "Image_Request_CC_Project_1";

	public static final Integer MAX_VISIBILITY_TIMEOUT = 40;

	public static final Integer MAX_WAIT_TIME_OUT = 20;

	public static final String INPUT_OUTPUT_SEPARATOR = "---";
	
	public static final Integer MAX_NUMBER_OF_THREAD = 250; 
	
	public static final Integer MAX_NUMBER_OF_ACCEPTED_THREAD = 20;
}
