package com.awsiaasproject.imagerecognization.webapp;

import com.awsiaasproject.imagerecognization.webapp.config.AppConfig;
import com.awsiaasproject.imagerecognization.webapp.constant.ProjectConstant;
import com.awsiaasproject.imagerecognization.webapp.service.AWSUtil;
import com.awsiaasproject.imagerecognization.webapp.service.BusinessLogic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/*
 * Main class to run the controller to auto scale up when the demand increases.
 */
@SpringBootApplication
@EnableAutoConfiguration
public class WebappApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebappApplication.class, args);

		// Defining Spring IoC controller and configuring all the beans require for the
		// project.
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

		
		AWSUtil awsService = context.getBean(AWSUtil.class);
		BusinessLogic backendService = context.getBean(BusinessLogic.class);

	//Remove below multiline comment when you want to reset and configuring your basic AWS in your account.
	/*
		System.out.println("Reseting AWS Services");
		awsService.resetBasicAWSServices(ProjectConstant.INPUT_QUEUE, ProjectConstant.OUTPUT_QUEUE,
				ProjectConstant.INPUT_BUCKET, ProjectConstant.OUTPUT_BUCKET);
		try {
			Thread.sleep(60000);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("Starting AWS Services");
		awsService.configuredBasicAWSServices(ProjectConstant.INPUT_QUEUE, ProjectConstant.OUTPUT_QUEUE,
				ProjectConstant.INPUT_BUCKET, ProjectConstant.OUTPUT_BUCKET);
	*/

		/*
		 * Thread 1 will run the run the controller to auto scale up.
		 * Thread 2 will run to put response from output queue to hashmap.
		 */
		Thread worker1 = new Thread((Runnable) awsService);
		Thread worker2 = new Thread((Runnable) backendService);
		System.out.println("Thread 1 Starting");
		worker1.start();
		System.out.println("Thread 2 Starting");
		worker2.start();
		System.out.println("Completed");
		context.close();
	}
}