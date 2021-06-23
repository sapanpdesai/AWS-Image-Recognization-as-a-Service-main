package com.awsiaasproject.imagerecognization.apptier_consumer;

import com.awsiaasproject.imagerecognization.apptier_consumer.config.AppConfig;
import com.awsiaasproject.imagerecognization.apptier_consumer.constant.ProjectConstant;
import com.awsiaasproject.imagerecognization.apptier_consumer.service.AWSUtil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
/*
 * Starting point of app instances.
 * Here we have implemented logic of threading to decide how many thread it should generate to serve user request.
 * We have decided to put maximum number of thread to 250 due to limitation of AWS EC2 instance as thread uses RAM.
 * Threading logic is based on number of messages present in request queue. 
 * If messages are >= 19*x then we will generate x threads. 
 * We have used bunch of try catch to handle creation of threads. 
 * 
 * What you can improve? -->
 * You can stop the thread and output "no prediction" to user if particular request take more than desired time.
 * You don't want your app instance to run forever. 
 */
@SpringBootApplication
@EnableAutoConfiguration
public class AppTierConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppTierConsumerApplication.class, args);
		Integer NUMBER_OF_THREAD = 1;
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
			AWSUtil awsService = context.getBean(AWSUtil.class);

			Integer TOTAL_NUMBER_OF_MSG_IN_INPUT_QUEUE = awsService
					.getTotalNumberOfMessagesInQueue(ProjectConstant.INPUT_QUEUE);
			
			if (ProjectConstant.MAX_NUM_OF_APP_INSTANCES < TOTAL_NUMBER_OF_MSG_IN_INPUT_QUEUE) 
				NUMBER_OF_THREAD = TOTAL_NUMBER_OF_MSG_IN_INPUT_QUEUE / ProjectConstant.MAX_NUM_OF_APP_INSTANCES;
			
			if(ProjectConstant.MAX_NUMBER_OF_THREAD < NUMBER_OF_THREAD)
				NUMBER_OF_THREAD = ProjectConstant.MAX_NUMBER_OF_THREAD;
			
			try {
				for (int t = 0; t < NUMBER_OF_THREAD; t++) {
					Thread thread = new Thread((Runnable) awsService);
					thread.start();
					thread.join();
				}
			}
			catch(Exception a){
				try {
					for (int t = 0; t < ProjectConstant.MAX_NUMBER_OF_ACCEPTED_THREAD; t++) {
						Thread thread = new Thread((Runnable) awsService);
						thread.start();
						thread.join();
					}
				}
				catch(Exception p){
					awsService.scaleIn();
				}
			}
			

			awsService.terminateInstance();
			context.close();
		} catch (Exception e) {
			System.out.println("Problem in logic");
			e.printStackTrace();
		}

	}
}
